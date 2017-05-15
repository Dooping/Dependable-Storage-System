package srv;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import actors.FaultDetection;
import actors.Proxy;
import actors.Replica;
import actors.Spawner;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.apache.commons.cli.*;



public class SSLRestServer {
	public static void main(String[] args) throws Exception {
		Config defaultCfg = ConfigFactory.load();
		
		Options options = new Options();
		
		Option typeOp = new Option("t", "type", true, "type of server (spawner1/spawner2/proxy)");
		typeOp.setRequired(true);
        options.addOption(typeOp);
        
        Option nOp = new Option("n", "number", true, "number of replicas to spawn");
        nOp.setRequired(false);
        options.addOption(nOp);
        
        Option tOp = new Option("th", "threshold", true, "number of votes to kick a replica");
        tOp.setRequired(false);
        options.addOption(tOp);
        
        Option sOp = new Option("s", "sentinent", true, "number of sentinent replicas to spawn");
        sOp.setRequired(false);
        options.addOption(sOp);
        
        Option fOp = new Option("f", "fault", true, "fault detection server's address");
        fOp.setRequired(false);
        options.addOption(fOp);

        Option crashOp = new Option("cr", "crash", true, "number of replicas to crash");
        crashOp.setRequired(false);
        options.addOption(crashOp);
        
        Option byzantineOp = new Option("bz", "byzantine", true, "number of replicas that are byzantine");
        byzantineOp.setRequired(false);
        options.addOption(byzantineOp);

        Option chanceOp = new Option("ch", "chance", true, "probability of crashing/byzantine error");
        chanceOp.setRequired(false);
        options.addOption(chanceOp);
        
        Option quorumOp = new Option("q", "quorum", true, "quorum size");
        quorumOp.setRequired(false);
        options.addOption(quorumOp);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("SSLRestServer", options);

            System.exit(1);
            return;
        }
        
		int n = Integer.parseInt(cmd.getOptionValue("number", "4"));
		int s = Integer.parseInt(cmd.getOptionValue("sentinent", "0"));
		int threshold = Integer.parseInt(cmd.getOptionValue("threshold", "10"));
		String fault = cmd.getOptionValue("fault", "akka.ssl.tcp://FaultDetection@localhost:2563/user/faultDetection");
		int crash = Integer.parseInt(cmd.getOptionValue("crash", "0"));
		int chance = Integer.parseInt(cmd.getOptionValue("chance", "0"));
		int quorum = Integer.parseInt(cmd.getOptionValue("quorum", "5"));
		int byzantine = Integer.parseInt(cmd.getOptionValue("byzantine", "0"));
		
		
		String type = cmd.getOptionValue("type");
		switch(type){
		case "spawner1":
			ActorSystem spawner1 = ActorSystem.create("Spawner1",ConfigFactory.load().getConfig("Spawner1").withFallback(defaultCfg));
			System.out.println("Spawner1 created...");
			spawner1.actorOf(Props.create(Spawner.class, n, s, fault),"spawner");
			break;
		case "spawner2":
			ActorSystem spawner2 = ActorSystem.create("Spawner2",ConfigFactory.load().getConfig("Spawner2").withFallback(defaultCfg));
			System.out.println("Spawner2 created...");
			spawner2.actorOf(Props.create(Spawner.class, n, s, fault),"spawner");
			break;
		case "fault":
			ActorSystem faultDetection = ActorSystem.create("FaultDetection",ConfigFactory.load().getConfig("FaultDetection").withFallback(defaultCfg));
			System.out.println("Fault Detection Server created...");
			faultDetection.actorOf(Props.create(FaultDetection.class, threshold),"faultDetection");
			break;
		case "proxy":
			URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
			ResourceConfig config = new ResourceConfig();
			ActorSystem system = ActorSystem.create("Proxy",ConfigFactory.load().getConfig("Proxy").withFallback(defaultCfg));
			system.actorOf(Props.create(Proxy.class, crash, byzantine, chance, quorum, fault),"proxy");
			config.register(new AbstractBinder() {
	            protected void configure() {
	                bind(system).to(ActorSystem.class);
	            }
	        });
			config.register( new ServerResource());

			HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
			System.err.println("SSL REST Server ready... @ " + InetAddress.getLocalHost().getHostAddress());
			break;
		}
		
		
		
	}
}