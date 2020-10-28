package com.kuta.akka.gateway;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

public class OnlineActor extends AbstractActor {
	
	private int count;
	private HashSet<ActorRef> actors;
	private Map<Long, ActorWrapper> actorMap;
	{
		actors = new HashSet<>();
		actorMap = new HashMap<Long, ActorWrapper>();
	}
	
	@Override
	public Receive createReceive() {
		// TODO Auto-generated method stub
		return receiveBuilder()
				.match(UpdownInfo.class, (x) -> {
			if(x.isUp()) {
				actors.add(x.getActorRef());
				if(x.getUuid()>0) {
					ActorWrapper wrapper = new ActorWrapper();
					wrapper.setActor(x.getActorRef());
					wrapper.setUpTime(new Date());
					actorMap.put(x.getUuid(), wrapper);
				}
				count++;
			} else {
				actors.remove(x.getActorRef());
				if(x.getUuid()>0) {
					actorMap.remove(x.getUuid());
				}
				count--;
			}
		}).build();
	}
	
	
}
