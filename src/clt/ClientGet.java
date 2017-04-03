package clt;

import Datatypes.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public class ClientGet {

	public static void main(String[] args) throws Exception {
		String hostname = "localhost:9090";
		if( args.length > 0)
			hostname = args[0];

		Client client = ClientBuilder.newBuilder()
				.hostnameVerifier(new InsecureHostnameVerifier())
				.build();

		URI baseURI = UriBuilder.fromUri("http://" + hostname + "/").build();
		WebTarget target = client.target(baseURI);

		String value = target.path("/server")
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get(String.class);

		System.err.println("Value: " + value);
		
		Response key = target.path("/server/putset")
				.request().header("key", "jadhgagj").post(Entity.entity(new Entry(1,"2",3,"4",5,"6"), MediaType.APPLICATION_JSON));
		
		System.err.println("Key:");
		System.err.println(key.getEntity());
		
	}

	static public class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
}
