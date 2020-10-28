package com.kuta.akka.gateway;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;





/**
 * 全局配件
 * */
public class Globals {
	private static ActorRef onlineActor;
	private static ActorSystem system;
	private static ActorRef rootActor;
	private static ActorRef roomActor;
	private static ActorRef raceActor;
	private static ActorRef hallRouter;
	public static ActorRef getOnlineActor() {
		return onlineActor;
	}
	public static ActorSystem getSystem() {
		return system;
	}
	public static ActorRef getRoot() {
		return rootActor;
	}
	public static ActorRef getRoomActor() {
		return roomActor;
	}
	public static ActorRef getRaceActor() {
		return raceActor;
	}
	public static ActorRef getHallRouter() {
		return hallRouter;
	}
	
	public static void mount(ActorSystem actorSystem, ActorRef rootActor, ActorRef roomActor, ActorRef raceActor,ActorRef hallRouter) {
		system = actorSystem;
		onlineActor = system.actorOf(Props.create(OnlineActor.class));
		Globals.rootActor = rootActor;
		Globals.roomActor = roomActor;
		Globals.raceActor = raceActor;
		Globals.hallRouter = hallRouter;
	}
}
