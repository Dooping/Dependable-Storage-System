package srv;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.cli.*;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import actors.Proxy;
import actors.Replica;
import actors.Test;
import akka.actor.*;

public class SSLRestServer0 {
	static final File KEYSTORE = new File("./home/csd/server.jks");
	static final char[] JKS_PASSWORD = "changeit".toCharArray();
	static final char[] KEY_PASSWORD = "changeit".toCharArray();

	public static void main(String[] args) throws Exception {
		
Config defaultCfg = ConfigFactory.load();
		
		Options options = new Options();
		
		Option keystoreOp = new Option("k", "keystore", true, "keystore path");
		keystoreOp.setRequired(false);
        options.addOption(keystoreOp);
		
		Option typeOp = new Option("t", "type", true, "type of server (spawner1/spawner2/proxy)");
		typeOp.setRequired(true);
        options.addOption(typeOp);
        
        Option nOp = new Option("n", "number", true, "number of replicas to spawn");
        nOp.setRequired(false);
        options.addOption(nOp);

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
		int crash = Integer.parseInt(cmd.getOptionValue("crash", "0"));
		int chance = Integer.parseInt(cmd.getOptionValue("chance", "0"));
		int quorum = Integer.parseInt(cmd.getOptionValue("quorum", "5"));
		int byzantine = Integer.parseInt(cmd.getOptionValue("byzantine", "0"));
		File keystore = KEYSTORE;
		if (cmd.hasOption("keystore"))
			keystore = new File(cmd.getOptionValue("keystore"));
		
		
		String type = cmd.getOptionValue("type");
		switch(type){
		case "spawner1":
			ActorSystem spawner1 = ActorSystem.create("Spawner1",ConfigFactory.load().getConfig("Spawner1").withFallback(defaultCfg));
			System.out.println("Spawner1 created...");
			for(int i = 1; i <= n; i++)
				spawner1.actorOf(Props.create(Replica.class),"r"+i);
			break;
		case "spawner2":
			ActorSystem spawner2 = ActorSystem.create("Spawner2",ConfigFactory.load().getConfig("Spawner2").withFallback(defaultCfg));
			System.out.println("Spawner2 created...");
			for(int i = 1; i <= n; i++)
				spawner2.actorOf(Props.create(Replica.class),"r"+i);
			break;
		case "proxy":
			URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(9090).build();
			ResourceConfig config = new ResourceConfig();
			ActorSystem system = ActorSystem.create("Proxy",ConfigFactory.load().getConfig("Proxy").withFallback(defaultCfg));
			system.actorOf(Props.create(Proxy.class, crash, byzantine, chance, quorum),"proxy");
			config.register(new AbstractBinder() {
	            protected void configure() {
	                bind(system).to(ActorSystem.class);
	            }
	        });
			config.register( new ServerResource());

			SSLContext sslContext = SSLContext.getInstance("TLSv1");

			KeyStore keyStore = KeyStore.getInstance("JKS");
			try (InputStream is = new FileInputStream(keystore)) {
				keyStore.load(is, JKS_PASSWORD);
			}

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, KEY_PASSWORD);
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);

			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

			HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config, true, new SSLEngineConfigurator(sslContext));
			System.err.println("SSL REST Server ready... @ " + InetAddress.getLocalHost().getHostAddress());
			break;
		}
		
		
		

		
		
		
	}
}