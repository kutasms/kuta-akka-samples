package com.kuta.akka.gateway;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.kuta.akka.base.entity.KutaAkkaConstants;
import com.kuta.akka.base.entity.GatewayMessage;
import com.kuta.akka.base.entity.RaceforwardMessage;
import com.kuta.akka.base.entity.RoomforwardMessage;
//import com.kuta.akka.base.event.ExecResultByActorEvent;
import com.kuta.base.util.KutaUtil;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.ws.TextMessage;
import akka.routing.FromConfig;
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope;

public class GatewayActor extends AbstractActor {

	LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
	private ActorRef hallRouter;
	private ActorRef channel;
	private int keySeed = 0;
	/**
	 * 是否已登录
	 */
	private boolean logged;
	private long uuid = 0;
	/**
	 * 集群中处理房间业务Actor
	 */
	private ActorRef roomActor;
	/**
	 * 集群中处理比赛业务的Actor
	 */
	private ActorRef raceActor;

	private Long roomId;
	private Integer raceId;

	public static Props props(ActorRef hallRouter) {
		return Props.create(GatewayActor.class, hallRouter);
	}
	
	public GatewayActor(ActorRef hallRouter) {
		this.hallRouter = hallRouter;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(ActorRef.class, this::init).match(String.class, str -> {
			switch (str) {
			case "ack":
				break;
			default:
				break;
			}
		}).match(TextMessage.class, x -> {
			if(KutaUtil.isValueNull(x)) {
				logger.error("出现错误，TextMessage:null");
				return;
			}
			onMessage(x.getStrictText());
		})
//		.match(ExecResultByActorEvent.class, x -> {
//			/**
//			 * code=300000 创建房间
//			 */
//			if (x.isSuccess() && x.getCode().equals(300000)) {
//				this.roomActor = x.getExecutor();
//				this.roomId = x.getProps().getLong(KutaAkkaConstants.PARAM_ROOM_ID);
//			}
//			/**
//			 * code=400000 创建比赛
//			 */
//			if (x.isSuccess() && x.getCode().equals(400000)) {
//				this.raceActor = x.getExecutor();
//				this.raceId = x.getProps().getInteger(KutaAkkaConstants.PARAM_RACE_ID);
//			}
//		})
		.matchAny(x -> {
			executeNoImplementMsg(x);
		}).build();
	}

	@Override
	public void postStop() {
		// 当用户断线的时候会进入该方法
		Globals.getOnlineActor().tell(new UpdownInfo(false, this.uuid, this.channel), self());

	}

	private void executeNoImplementMsg(Object obj) {
		logger.info("收到未实现的业务消息:{}", obj.getClass().getName());
	}

	private void init(ActorRef actor) {
		this.channel = actor;
		Globals.getOnlineActor().tell(new UpdownInfo(true, 0, this.channel), self());
	}

	/**
	 * 处理收到的消息
	 */
	private void onMessage(String message) {
//		if (hallRouter == null) {
//			hallRouter = getContext().actorOf(FromConfig.getInstance().props(), "hall-router");
//		}
		if(KutaUtil.isEmptyString(message)) {
			logger.warning("客户端传送了空数据");
			return;
		}
		if(!message.equals("keep")) {
			logger.info("CLIENT MSG:{}", message);
		}
		if (message.equals("keep")) {
//			logger.info("收到来自客户端的ping消息：{}", message);
			this.channel.tell("pong", self());
			return;
		}
		try {

			JSONObject jsonObject = JSONObject.parseObject(message);
			String cmd = jsonObject.getString(KutaAkkaConstants.PARAM_CMD);
			String module = jsonObject.getString(KutaAkkaConstants.PARAM_MODULE);
			Integer code = jsonObject.getInteger(KutaAkkaConstants.PARAM_CODE);
			/**
			 * 当命令编码>=0 && <10000时属于自身命令，不会执行forword
			 */
			if (code >= 0 && code < 10000) {
				return;
			}

			if (code > 300000 && code < 400000) {
				RoomforwardMessage rfm = new RoomforwardMessage();
				rfm.setChannel(this.channel);
				rfm.setProxy(self());
				rfm.setCode(code);
				rfm.setParams(jsonObject);
				rfm.setRoomId(this.roomId);
				Globals.getRoomActor().tell(rfm, self());
				return;
			}

			if (code > 400000 && code <= 500000) {
				RaceforwardMessage rfm = new RaceforwardMessage();
				rfm.setChannel(this.channel);
				rfm.setProxy(self());
				rfm.setCode(code);
				rfm.setParams(jsonObject);
				rfm.setRaceId(this.raceId);
				Globals.getRaceActor().tell(rfm, self());
				return;
			}

			GatewayMessage msg = new GatewayMessage();
			msg.setChannel(this.channel);
			msg.setProxy(self());
			msg.setParams(jsonObject);
			msg.setCode(code);
			if(jsonObject.containsKey("pid")) {
				Globals.getHallRouter().tell(new ConsistentHashableEnvelope(msg, jsonObject.get("pid")), self());
			}
			else {
				keySeed++;
				Globals.getHallRouter().tell(new ConsistentHashableEnvelope(msg, String.format("K%s", keySeed)), self());
			}
			
		} catch (JSONException e) {
			// TODO: handle exception
			this.channel.tell("请求失败,数据格式错误!", self());
		}
	}
}