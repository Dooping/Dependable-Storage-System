package srv;

import java.net.InetAddress;
import java.net.URI;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.net.httpserver.HttpServer;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import actors.Test;


public class SSLRestServer {
	public static void main(String[] args) throws Exception {
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
		ResourceConfig config = new ResourceConfig();
		config.register( new ServerResource());

		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config);
		System.err.println("SSL REST Server ready... @ " + InetAddress.getLocalHost().getHostAddress());
		
		final ActorSystem system = ActorSystem.create("MySystem");
		final ActorRef test = system.actorOf(Props.create(Test.class),
		  "myactor");
		
	}
}