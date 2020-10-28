package com.kuta.akka.gateway;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import com.kuta.akka.base.KutaActor;
import com.kuta.akka.base.entity.GatewayMessage;
import com.kuta.akka.base.entity.HttpResponseMessage;
import com.kuta.akka.base.entity.RegistrationMessage;
import com.kuta.base.util.AESUtil;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope;

public class RESTfulActor extends KutaActor {
	private long seed = 0;
	private final ActorRef gameRouter;
	private static final Map<String, String> appKeySecretMap = new HashMap<>();
	private static final String APP_KEY_WALLET = "ifzZi29MM2";
	private static final String APP_SECRET_WALLET = "381nf87vli13658m";
	private static final String APP_KEY_GAME_BACKEND = "x97mvAi3o1";
	private static final String APP_SECRET_GAME_BACKEND = "9m1lm3i39z7d9q8g";
	static {
		appKeySecretMap.put(APP_KEY_WALLET, APP_SECRET_WALLET);
		appKeySecretMap.put(APP_KEY_GAME_BACKEND, APP_SECRET_GAME_BACKEND);
	}

	public RESTfulActor(ActorRef gameRouter) {
		// TODO Auto-generated constructor stub
		this.gameRouter = gameRouter;
	}

	public static Props props(ActorRef gameRouter) {
		return Props.create(RESTfulActor.class, gameRouter);
	}

	@Override
	public void onReceive(ReceiveBuilder rb) {
		// TODO Auto-generated method stub
		rb.match(String.class, msg -> {
		}).match(JSONObject.class, msg -> {
//			logger.info(String.format("RESTful received:%s", msg.toJSONString()));
			String[] pmStrings = new String[] { "app_key", "platform", "params" };
			for (String string : pmStrings) {
				if (!msg.containsKey(string)) {
					sender().tell(new HttpResponseMessage(null, -1, "参数丢失"), self());
					return;
				}
			}
			String appKey = msg.getString("app_key");
			if (!appKeySecretMap.containsKey(appKey)) {
				sender().tell(new HttpResponseMessage(null, -2, "app_key错误"), self());
				return;
			}
			if (appKey.equals(APP_KEY_WALLET)) {
				String decodeStr = null;
				try {
					decodeStr = AESUtil.Decrypt(msg.getString("params"), appKeySecretMap.get(APP_KEY_WALLET));
//					logger.info("解密信息:{}",decodeStr);
					JSONObject decodeJson = JSONObject.parseObject(decodeStr);
					if (!decodeJson.containsKey("code")) {
						sender().tell(new HttpResponseMessage(null, -5, "code丢失"), self());
						return;
					}

					Integer code = decodeJson.getInteger("code");

					if (code < 1000000 || code > 1100000) {
						sender().tell(new HttpResponseMessage(null, -4, "您请求的code不可解析.."), self());
						return;
					}
					
					GatewayMessage message = new GatewayMessage();
					message.setChannel(sender());
					message.setCode(code);
					message.setParams(decodeJson);
					message.setProxy(self());
//					logger.info("向工作端发送请求");
					gameRouter.tell(new ConsistentHashableEnvelope(message, String.format("K%s", ++seed)), self());
				} catch (Exception e) {
					// TODO: handle exception
					sender().tell(new HttpResponseMessage(null, -3, "数据解密错误"), self());
					return;
				}
			} else if(appKey.equals(APP_KEY_GAME_BACKEND)) {
				String decodeStr = null;
				try {
					decodeStr = AESUtil.Decrypt(msg.getString("params"), 
							appKeySecretMap.get(APP_KEY_GAME_BACKEND));
//					logger.info("解密信息:{}",decodeStr);
					JSONObject decodeJson = JSONObject.parseObject(decodeStr);
					if (!decodeJson.containsKey("code")) {
						sender().tell(new HttpResponseMessage(null, -5, "code丢失"), self());
						return;
					}

					Integer code = decodeJson.getInteger("code");

					if (code < 1100000 || code > 1200000) {
						sender().tell(new HttpResponseMessage(null, -4, "您请求的code不可解析.."), self());
						return;
					}
					
					GatewayMessage message = new GatewayMessage();
					message.setChannel(sender());
					message.setCode(code);
					message.setParams(decodeJson);
					message.setProxy(self());
//					logger.info("向工作端发送请求");
					gameRouter.tell(new ConsistentHashableEnvelope(message, String.format("K%s", ++seed)), self());
				} catch (Exception e) {
					// TODO: handle exception
					sender().tell(new HttpResponseMessage(null, -3, "数据解密错误"), self());
					return;
				}
			}

		});
	}

	@Override
	public void onRegister(RegistrationMessage msg) {
		// TODO Auto-generated method stub

	}

}
