package com.kuta.akka.gateway;
import com.kuta.akka.base.KutaActorWithCluster;
import com.kuta.akka.base.entity.KutaAkkaConstants;
import com.kuta.akka.base.entity.RegistrationMessage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.FromConfig;

public class DistributeActor extends KutaActorWithCluster{

	Cluster cluster = Cluster.get(getContext().system());
	
	private final ActorRef hallRouter = getContext().actorOf(FromConfig.getInstance().props(), "hall-router");
	
	public DistributeActor(ActorSystem system) {
		ActorRef roomActor = getContext().actorOf(Props.create(RoomForwordActor.class), KutaAkkaConstants.ACTOR_ROOM_FORWORD);
		ActorRef raceActor = getContext().actorOf(Props.create(RaceForwordActor.class), "race-forward-actor");
		Globals.mount(system, self(), roomActor, raceActor, hallRouter);
		
		logger.info("网关已经启动...");
	}
	
	@Override
	public void preStart() {
		// TODO Auto-generated method stub
		super.preStart();
		getContext().system().eventStream().subscribe(self(), DeadLetter.class);
	}
	
	@Override
	public void onReceive(ReceiveBuilder rb) {
		// TODO Auto-generated method stub
		rb.match(DeadLetter.class, msg->{
			logger.info("收到死信：{}", msg.message().toString());
		});
	}

	@Override
	public void onRegister(RegistrationMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSelfUp() {
		// TODO Auto-generated method stub
		
	}
}
