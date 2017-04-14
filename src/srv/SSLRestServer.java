package srv;

import java.net.InetAddress;
import java.net.URI;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import actors.Child;
import akka.actor.ActorSystem;
import akka.actor.Props;


public class SSLRestServer {
	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("ExampleSystem");
		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
		ResourceConfig config = new ResourceConfig();
		
		system.actorOf(Props.create(Child.class),"child");

		config.register(new AbstractBinder() {
            protected void configure() {
                bind(system).to(ActorSystem.class);
            }
        });
		
		config.register( new ServerResource());

		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
		System.err.println("SSL REST Server ready... @ " + InetAddress.getLocalHost().getHostAddress());
		
		Helper actor = new Helper();
		
	}
}