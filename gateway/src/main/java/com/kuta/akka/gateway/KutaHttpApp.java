package com.kuta.akka.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.kuta.akka.base.entity.KutaAkkaConstants;
import com.kuta.akka.base.entity.HttpResponseMessage;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.typed.javadsl.AskPattern;
import akka.dispatch.Futures;
import akka.dispatch.MessageDispatcher;
import akka.dispatch.OnComplete;
import akka.http.javadsl.ConnectionContext;
import akka.http.javadsl.HttpsConnectionContext;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.coding.Coder;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.pattern.AskableActorRef;
import akka.pattern.Patterns;
import akka.stream.CompletionStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.concurrent.Await;
import scala.concurrent.CanAwait;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
/**
 * Hello world!
 *
 */
public class KutaHttpApp extends HttpApp 
{
	private Logger logger = LoggerFactory.getLogger(KutaHttpApp.class);
	private ActorSystem system;
	final Marshaller<HttpResponseMessage, HttpResponse> marshaller = Marshaller.entityToOKResponse(Jackson.<HttpResponseMessage>marshaller());
	
	public KutaHttpApp(ActorSystem system) {
		this.system = system;
	}
	
	private final Duration timeout = Duration.ofSeconds(8l);

	@Override
	protected void postHttpBinding(ServerBinding binding) {
		// TODO Auto-generated method stub
		
		super.postHttpBinding(binding);
		
	}

	public Route RESTfulAsyncRouter() {
		return path(PathMatchers.segment("RESTful"),()->
		   concat(
			post(()->{
			   return entity(Jackson.unmarshaller(JSONObject.class), json -> {
					final Set<HttpHeader> headers = new HashSet<>();
					headers.add(HttpHeader.parse("Access-Control-Allow-Origin", "*"));
					headers.add(HttpHeader.parse("Access-Control-Allow-Credentials", "true"));
					headers.add(HttpHeader.parse("Access-Control-Allow-Methods", "POST,OPTIONS"));
					headers.add(HttpHeader.parse("Access-Control-Expose-Headers", "Content-Type, Access-Control-Allow-Origin, Access-Control-Allow-Credentials"));
					headers.add(HttpHeader.parse("Access-Control-Allow-Headers", "Content-Type,Access-Token,Authorization"));
					return completeWith(Marshaller.entityToOKResponse(headers, Jackson.<HttpResponseMessage>marshaller()), f->{
						system.actorOf(RESTfulAsyncActor.props(json, f));
					});
				});
		   }), 
		   options(()->{
				List<HttpHeader> list = new ArrayList<>();
				list.add(HttpHeader.parse("Access-Control-Allow-Origin", "*"));
				list.add(HttpHeader.parse("Access-Control-Allow-Credentials", "true"));
				list.add(HttpHeader.parse("Access-Control-Allow-Methods", "POST,OPTIONS"));
				list.add(HttpHeader.parse("Access-Control-Expose-Headers", "Content-Type, Access-Control-Allow-Origin, Access-Control-Allow-Credentials,Vary"));
				list.add(HttpHeader.parse("Access-Control-Allow-Headers", "Content-Type,Access-Token,Authorization"));
				return respondWithHeaders(list,()-> complete(""));
			})));
	}
	
	public Route RESTfulRouter() {
		return path(PathMatchers.segment("RESTful"), 
				()-> concat(
						post(()->{
							return entity(Jackson.unmarshaller(JSONObject.class), json -> {
								 final JSONObject data = json;
								 final MessageDispatcher dispatcher = system.dispatchers().lookup(KutaAkkaConstants.BLOCKING_DISPATCHER);
								 try {
									return 
											 CompletableFuture.<Route>supplyAsync(()->{
													final ActorRef RESTfull = system.actorOf(RESTfulActor.props(Globals.getHallRouter())
															.withDispatcher(KutaAkkaConstants.BLOCKING_DISPATCHER));
//													logger.info("当前运行线程:{}",Thread.currentThread());
													CompletionStage<Optional<HttpResponseMessage>> rsp = Patterns
															.ask(RESTfull, data, timeout)
															.thenApply(a -> {
																return Optional.of((HttpResponseMessage) a);
															});
													return onSuccess(() -> rsp, performed -> {
														RESTfull.tell(PoisonPill.getInstance(), ActorRef.noSender());
														List<HttpHeader> list = new ArrayList<>();
														list.add(HttpHeader.parse("Access-Control-Allow-Origin", "*"));
														list.add(HttpHeader.parse("Access-Control-Allow-Credentials", "true"));
														list.add(HttpHeader.parse("Access-Control-Allow-Methods", "POST,OPTIONS"));
														list.add(HttpHeader.parse("Access-Control-Expose-Headers", "Content-Type, Access-Control-Allow-Origin, Access-Control-Allow-Credentials"));
														list.add(HttpHeader.parse("Access-Control-Allow-Headers", "Content-Type,Access-Token,Authorization"));
														if (performed.isPresent()) {
															if(performed.get().getSize() > (1024 * 50)) {
																return encodeResponseWith(
																		Collections.singletonList(Coder.Gzip),
																		()->complete(StatusCodes.OK, list, performed.get(), Jackson.marshaller())
																		);
															}
															else {
																return complete(StatusCodes.OK, list, performed.get(), Jackson.marshaller());
															}
														}
														else {
															return complete(StatusCodes.NOT_FOUND);
														}
												}).orElse(complete(StatusCodes.INTERNAL_SERVER_ERROR));
												}, dispatcher).get();
								} catch (InterruptedException | ExecutionException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								return null;
							});
						})
						, options(()->{
							List<HttpHeader> list = new ArrayList<>();
							list.add(HttpHeader.parse("Access-Control-Allow-Origin", "*"));
							list.add(HttpHeader.parse("Access-Control-Allow-Credentials", "true"));
							list.add(HttpHeader.parse("Access-Control-Allow-Methods", "POST,OPTIONS"));
							list.add(HttpHeader.parse("Access-Control-Expose-Headers", "Content-Type, Access-Control-Allow-Origin, Access-Control-Allow-Credentials,Vary"));
							list.add(HttpHeader.parse("Access-Control-Allow-Headers", "Content-Type,Access-Token,Authorization"));
							return respondWithHeaders(list,()-> complete(""));
						})
						)
				);
	}

	public Route websocketRouter() {
		return path("gateway", () -> get(() -> handleWebSocketMessages(chatRoomSocketFlow())));
	}
	
	public Route helloworldRouter() {
		return path("hello", ()->get(()->complete("hello world")));
	}
	
	public Route staticResourceRouter() {
		return path(PathMatchers.remaining(), remain -> get(()-> {
			if(remain.endsWith(".properties") || remain.endsWith(".xml") || remain.endsWith(".conf")) {
				return complete(StatusCodes.UNAUTHORIZED);
			}
			return getFromResource(remain);
			}));
	}

	
	@Override
	protected Route routes() {
		// TODO Auto-generated method stub
		return route(staticResourceRouter(), helloworldRouter(),websocketRouter(),RESTfulAsyncRouter());
	}

	private Flow<Message, Message, NotUsed> chatRoomSocketFlow() {
		ActorRef actor = system.actorOf(GatewayActor.props(Globals.getHallRouter()));
		return socketFlow(actor);
	}

	// 串联处理stream对象 SOURCE SINK FLOW
	private Flow<Message, Message, NotUsed> socketFlow(ActorRef actor) {

		// 背压支撑
		Source<Message, NotUsed> source = Source.<String>actorRefWithBackpressure("ack", o -> {
			if (o == "complete")
				return Optional.of(CompletionStrategy.draining());
			else
				return Optional.empty();
		}, o -> Optional.empty()).map(message -> (Message) TextMessage.create(message))
				.mapMaterializedValue(textMessage -> {
					actor.tell(textMessage, ActorRef.noSender());
					return NotUsed.getInstance();
				})
		// .keepAlive(Duration.ofSeconds(10), () -> TextMessage.create("{}"))
		;

		Sink<Message, NotUsed> sink = Flow.<Message>create().to(Sink.actorRef(actor, PoisonPill.getInstance()));

		return Flow.fromSinkAndSource(sink, source);
	}
	
	public HttpsConnectionContext useHttps(ActorSystem system) {
	    HttpsConnectionContext https = null;
	    try {
	      // initialise the keystore
	      // !!! never put passwords into code !!!
	      final char[] password = "****".toCharArray();

	      final KeyStore ks = KeyStore.getInstance("PKCS12");
	      final InputStream keystore = KutaHttpApp.class.getClassLoader().getResourceAsStream("***.pfx");
	      if (keystore == null) {
	        throw new RuntimeException("Keystore required!");
	      }
	      ks.load(keystore, password);
	      Enumeration<String> aliases = ks.aliases();
			while(aliases.hasMoreElements()) {
				String next = aliases.nextElement();
				logger.info(next);
				java.security.Key key = ks.getKey(next, password);
				logger.info("alg:{},format:{}",key.getAlgorithm(),key.getFormat());
			}
	      
	      final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	      keyManagerFactory.init(ks, password);

	      final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
	      tmf.init(ks);

	      final SSLContext sslContext = SSLContext.getInstance("TLS");
	      sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

	      https = ConnectionContext.https(sslContext);

	    } catch (NoSuchAlgorithmException | KeyManagementException e) {
	      system.log().error("Exception while configuring HTTPS.", e);
	    } catch (CertificateException | KeyStoreException | UnrecoverableKeyException | IOException e) {
	      system.log().error("Exception while ", e);
	    }

	    return https;
	}
}
