package srv;

import java.net.InetAddress;
import java.net.URI;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import actors.Proxy;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


public class SSLRestServer {
	public static void main(String[] args) throws Exception {
		Config defaultCfg = ConfigFactory.load();
		ActorSystem spawner1 = ActorSystem.create("Spawner1",ConfigFactory.load().getConfig("Spawner1").withFallback(defaultCfg));
		ActorSystem spawner2 = ActorSystem.create("Spawner2",ConfigFactory.load().getConfig("Spawner2").withFallback(defaultCfg));
		ActorSystem system = ActorSystem.create("RemoteCreation",ConfigFactory.load().getConfig("RemoteCreation").withFallback(defaultCfg));
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
		ResourceConfig config = new ResourceConfig();
		
		system.actorOf(Props.create(Proxy.class),"proxy");

		config.register(new AbstractBinder() {
            protected void configure() {
                bind(system).to(ActorSystem.class);
            }
        });
		
		config.register( new ServerResource());

		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
		System.err.println("SSL REST Server ready... @ " + InetAddress.getLocalHost().getHostAddress());
		
		//Helper actor = new Helper();
		
	}
}