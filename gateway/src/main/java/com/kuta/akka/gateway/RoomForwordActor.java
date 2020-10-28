package com.kuta.akka.gateway;

import com.kuta.akka.base.KutaActor;
import com.kuta.akka.base.entity.RegistrationMessage;
//import com.kuta.akka.base.entity.RoomClosedMessage;
//import com.kuta.akka.base.entity.RoomCreatedMessage;
import com.kuta.akka.base.entity.RoomforwardMessage;
import com.kuta.akka.base.mapping.RoomActorMapping;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.japi.pf.ReceiveBuilder;

/**
 * 房间信息转发Actor
 * */
public class RoomForwordActor extends KutaActor{
	
	private RoomActorMapping mapping;
	
	public static Props props() {
		return Props.create(RoomForwordActor.class);
	}
	
	@Override
	public void preStart() throws Exception {
		// TODO Auto-generated method stub
		super.preStart();
	}
	
	
	
	@Override
	public void postStop() throws Exception {
		// TODO Auto-generated method stub
		super.postStop();
	}


	@Override
	public void onReceive(ReceiveBuilder rb) {
		// TODO Auto-generated method stub
		rb.match(RoomforwardMessage.class, msg->{
			ActorRef actorRef = mapping.get(msg.getRoomId());
			actorRef.tell(msg, sender());
		})
//		.match(RoomCreatedMessage.class, msg->{
//			logger.info("[CLIENT]收到ROOM创建完毕消息.");
//			if(mapping == null) {
//				mapping = new RoomActorMapping();
//			}
//			getContext().watch(msg.getActorRef());
//			mapping.put(msg.getRoomId(), msg.getActorRef());
//		})
//		.match(RoomClosedMessage.class, msg->{
//			ActorRef actorRef = mapping.get(msg.getRoomId());
//			mapping.remove(msg.getRoomId());
//			if(actorRef != null) {
//				getContext().unwatch(actorRef);
//			}
//		})
		.match(Terminated.class, msg->{
			logger.info("房间已经无法连接:{}",msg.getActor());
			mapping.terminal(msg.getActor());
			getContext().unwatch(msg.getActor());
		});
	}

	@Override
	public void onRegister(RegistrationMessage msg) {
		// TODO Auto-generated method stub
		
	}
	
	
}
