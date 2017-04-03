package srv;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Calendar;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.net.httpserver.HttpServer;

import actors.Test;
import akka.actor.*;

public class SSLRestServer0 {
	static final File KEYSTORE = new File("./home/csd/server.jks");
	static final char[] JKS_PASSWORD = "changeit".toCharArray();
	static final char[] KEY_PASSWORD = "changeit".toCharArray();

	public static void main(String[] args) throws Exception {
		File keystore = KEYSTORE;
		if( args.length > 0)
			keystore = new File(args[0]);

		URI baseUri = UriBuilder.fromUri("https://0.0.0.0/").port(9090).build();
		ResourceConfig config = new ResourceConfig();
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

		HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, config, sslContext);
		System.err.println("SSL REST Server ready... @ " + InetAddress.getLocalHost().getHostAddress());
		
		final ActorSystem system = ActorSystem.create("MySystem");
		final ActorRef myActor = system.actorOf(Props.create(Test.class),
		  "myactor");
	}
}