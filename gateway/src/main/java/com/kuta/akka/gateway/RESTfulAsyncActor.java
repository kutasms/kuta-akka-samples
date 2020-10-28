package com.kuta.akka.gateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSONObject;
import com.kuta.akka.base.KutaActor;
import com.kuta.akka.base.entity.GatewayMessage;
import com.kuta.akka.base.entity.HttpResponseMessage;
import com.kuta.akka.base.entity.RegistrationMessage;
import com.kuta.base.util.AESUtil;

import akka.actor.Props;
import akka.http.javadsl.model.HttpHeader;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope;

public class RESTfulAsyncActor extends KutaActor{

	private final Consumer<HttpResponseMessage> consumer;
	private final JSONObject reqMsg;
	private long seed = 0;
	private static final Map<String, String> appKeySecretMap = new HashMap<>();
	private static final String APP_KEY_WALLET = "x8kfeiialv";
	private static final String APP_SECRET_WALLET = "381nf87vli13mzld";
	private static final String APP_KEY_GAME_BACKEND = "x97jmLi3o1";
	private static final String APP_SECRET_GAME_BACKEND = "9m1l20N39z7d9q8g";
	static {
		appKeySecretMap.put(APP_KEY_WALLET, APP_SECRET_WALLET);
		appKeySecretMap.put(APP_KEY_GAME_BACKEND, APP_SECRET_GAME_BACKEND);
	}
	
	public RESTfulAsyncActor(JSONObject reqMsg,Consumer<HttpResponseMessage> consumer) {
		this.consumer = consumer;
		this.reqMsg = reqMsg;
	}
	
	public static Props props(JSONObject reqMsg,Consumer<HttpResponseMessage> consumer) {
		return Props.create(RESTfulAsyncActor.class,reqMsg,consumer);
	}
	
	
	@Override
	public void preStart() throws Exception {
		// TODO Auto-generated method stub
		super.preStart();
		this.toCluster();
	}

	private void toCluster() {
		String[] pmStrings = new String[] { "app_key", "platform", "params" };
		for (String string : pmStrings) {
			if (!reqMsg.containsKey(string)) {
				HttpResponseMessage rsp = new HttpResponseMessage(null, -1, "参数丢失");
				this.consumer.accept(rsp);
				getContext().stop(self());
				return;
			}
		}
		String appKey = reqMsg.getString("app_key");
		if (!appKeySecretMap.containsKey(appKey)) {
			HttpResponseMessage rsp = new HttpResponseMessage(null, -2, "app_key错误");
			this.consumer.accept(rsp);
			getContext().stop(self());
			return;
		}
		if (appKey.equals(APP_KEY_WALLET)) {
			String decodeStr = null;
			try {
				decodeStr = AESUtil.Decrypt(reqMsg.getString("params"), appKeySecretMap.get(APP_KEY_WALLET));
//				logger.info("解密信息:{}",decodeStr);
				JSONObject decodeJson = JSONObject.parseObject(decodeStr);
				if (!decodeJson.containsKey("code")) {
					HttpResponseMessage rsp = new HttpResponseMessage(null, -5, "code丢失");
					this.consumer.accept(rsp);
					getContext().stop(self());
					return;
				}

				Integer code = decodeJson.getInteger("code");

				if (code < 1000000 || code > 1100000) {
					HttpResponseMessage rsp = new HttpResponseMessage(null, -4, "您请求的code不可解析..");
					this.consumer.accept(rsp);
					getContext().stop(self());
					return;
				}
				
				GatewayMessage message = new GatewayMessage();
				message.setChannel(self());
				message.setCode(code);
				message.setParams(decodeJson);
				message.setProxy(self());
//				logger.info("向工作端发送请求");
				Globals.getHallRouter().tell(new ConsistentHashableEnvelope(message, String.format("K%s", ++seed)), self());
			} catch (Exception e) {
				// TODO: handle exception
				HttpResponseMessage rsp = new HttpResponseMessage(null, -3, "数据解密错误");
				this.consumer.accept(rsp);
				getContext().stop(self());
				return;
			}
		} else if(appKey.equals(APP_KEY_GAME_BACKEND)) {
			String decodeStr = null;
			try {
				decodeStr = AESUtil.Decrypt(reqMsg.getString("params"), 
						appKeySecretMap.get(APP_KEY_GAME_BACKEND));
//				logger.info("解密信息:{}",decodeStr);
				JSONObject decodeJson = JSONObject.parseObject(decodeStr);
				if (!decodeJson.containsKey("code")) {
					HttpResponseMessage rsp = new HttpResponseMessage(null, -5, "code丢失");
					this.consumer.accept(rsp);
					getContext().stop(self());
					return;
				}

				Integer code = decodeJson.getInteger("code");

				if (code < 1100000 || code > 1200000) {
					HttpResponseMessage rsp = new HttpResponseMessage(null, -4, "您请求的code不可解析..");
					this.consumer.accept(rsp);
					getContext().stop(self());
					return;
				}
				
				GatewayMessage message = new GatewayMessage();
				message.setChannel(self());
				message.setCode(code);
				message.setParams(decodeJson);
				message.setProxy(self());
//				logger.info("向工作端发送请求:{}", JSONObject.toJSONString(message));
				Globals.getHallRouter().tell(new ConsistentHashableEnvelope(message, String.format("K%s", ++seed)), self());
			} catch (Exception e) {
				// TODO: handle exception
				HttpResponseMessage rsp = new HttpResponseMessage(null, -3, "数据解密错误");
				this.consumer.accept(rsp);
				getContext().stop(self());
				return;
			}
		}
	}
	
	@Override
	public void onReceive(ReceiveBuilder rb) {
		// TODO Auto-generated method stub
		
		rb.match(HttpResponseMessage.class, msg->{
			this.consumer.accept(msg);
			getContext().stop(self());
		});
	}

	@Override
	public void onRegister(RegistrationMessage msg) {
		// TODO Auto-generated method stub
		
	}

}
