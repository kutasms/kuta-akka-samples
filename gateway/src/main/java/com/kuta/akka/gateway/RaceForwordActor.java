package com.kuta.akka.gateway;

import com.kuta.akka.base.entity.RaceCreatedMessage;
import com.kuta.akka.base.entity.RaceforwardMessage;
import com.kuta.akka.base.mapping.RaceActorMapping;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class RaceForwordActor extends AbstractActor {

	private RaceActorMapping mapping;

	public static Props props() {
		return Props.create(RaceForwordActor.class);
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
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder().match(RaceforwardMessage.class, msg->{
			ActorRef actorRef = mapping.get(msg.getRaceId());
			actorRef.forward(msg, getContext());
		}).match(RaceCreatedMessage.class, msg->{
			if(mapping == null) {
				mapping = new RaceActorMapping();
			}
			mapping.put(msg.getRaceId(), msg.getActorRef());
		}).build();
	}
}
