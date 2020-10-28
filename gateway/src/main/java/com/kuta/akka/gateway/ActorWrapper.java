package com.kuta.akka.gateway;

import java.util.Date;

import akka.actor.ActorRef;

public class ActorWrapper {
	
	private ActorRef actor;
	private Date upTime;

	public ActorRef getActor() {
		return actor;
	}

	public void setActor(ActorRef actor) {
		this.actor = actor;
	}

	public Date getUpTime() {
		return upTime;
	}

	public void setUpTime(Date upTime) {
		this.upTime = upTime;
	}
}
