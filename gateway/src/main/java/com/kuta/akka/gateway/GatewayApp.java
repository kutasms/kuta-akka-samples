package com.kuta.akka.gateway;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.kuta.akka.base.entity.GatewayMessage;
import com.kuta.akka.base.entity.KutaAkkaConstants;
import com.kuta.base.io.KutaStreamReader;
import com.kuta.common.config.utils.PropertyUtil;
import com.typesafe.config.ConfigFactory;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.settings.ServerSettings;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.serialization.Serializer;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

public class GatewayApp {
	private static final Logger logger = LoggerFactory.getLogger(GatewayApp.class);
	private static ActorSystem system;
	private static ActorRef root;
	public void run() {
		system = ActorSystem.create(KutaAkkaConstants.FRAMEWORK_NAME);
		root = system.actorOf(Props.create(DistributeActor.class, system), "gateway-actor");
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("\n");
		sBuilder.append("*****************************************\n");
		sBuilder.append("*    Welcome to Kuta Cluster Gateway    *\n");
		sBuilder.append("*****************************************\n");
		String host = PropertyUtil.getProperty("app", "gateway.host");
		Integer port = PropertyUtil.getInteger("app", "gateway.port");
		sBuilder.append(String.format("*       Bind Http To:%s:%s       *\n", host, port));
		sBuilder.append("*****************************************\n");
		logger.info(sBuilder.toString());
		GatewayApp clusterApplication = new GatewayApp();
		clusterApplication.run();
		
//		// 获取序列化 
//		Serialization serialization = SerializationExtension.get(system);
//		// 要序列化的对象
//		GatewayMessage original = new GatewayMessage();
//		original.setChannel(root);
//		original.setCode(3);
//		original.setParams(new JSONObject());
//		original.setProxy(root);
//		// 找到对应的序列化器
//		Serializer serializer = serialization.findSerializerFor(original);
//		// 将对象转换为字节数组
//		byte[] bytes = serializer.toBinary(original);
//		// 将字节数组转换回对象，类和类加载器都是null
//		GatewayMessage back = (GatewayMessage) serializer.fromBinary(bytes);
//		System.out.println(back.getChannel());
		
		KutaHttpApp app = new KutaHttpApp(system);
		

		final Http http = Http.get(system);
		final ActorMaterializer materializer = ActorMaterializer.create(system);
		final Flow<HttpRequest, HttpResponse, NotUsed> flow = app.routes().flow(system, materializer);
		http.bindAndHandle(flow, ConnectHttp.toHost(host, port), materializer);
		boolean useHttps = Boolean.parseBoolean(PropertyUtil.getProperty("app", "gateway.usessl")); // pick value from anywhere
		if (useHttps) {
			
			HttpsConnectionContext https = app.useHttps(system);
			http.setDefaultServerHttpContext(https);
			Integer sslPort = PropertyUtil.getInteger("app", "gateway.ssl.port");
			http.bindAndHandle(flow, ConnectHttp.toHost(host, sslPort), materializer);
			
			logger.info("启动ssl服务.host:{},port:{}",host,sslPort);
		}
		KutaStreamReader.readLine(System.in, consumer->{
			
		});
	}
}
