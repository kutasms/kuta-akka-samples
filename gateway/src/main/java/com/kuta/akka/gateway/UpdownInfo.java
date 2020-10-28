package com.kuta.akka.gateway;

import akka.actor.ActorRef;

public class UpdownInfo {
	
	public UpdownInfo(boolean isUp, long uuid, ActorRef actorRef) {
		this.up = isUp;
		this.uuid = uuid;
		this.actorRef = actorRef;
	}
	private boolean up;

	/**
	 * 是否上线操作,true:上线 false:下线
	 * */
	public boolean isUp() {
		return up;
	}

	public void setUp(boolean up) {
		this.up = up;
	}
	
	public long getUuid() {
		return uuid;
	}

	public void setUuid(long uuid) {
		this.uuid = uuid;
	}

	public ActorRef getActorRef() {
		return actorRef;
	}

	public void setActorRef(ActorRef actorRef) {
		this.actorRef = actorRef;
	}

	private long uuid;
	private ActorRef actorRef;
}
