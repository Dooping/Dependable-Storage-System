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

		//Test #1 [MAIN]
		String value = target.path("/server")
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get(String.class);
		System.out.println("Call: /server ; Response: " + value);
		
		//Test #2 [PUTSET]
		Response key = target.path("/server/putset")
				.request().header("key", "mykey").post(Entity.entity(new Entry(1,"2",3,"4",5,"6"), MediaType.APPLICATION_JSON));
		System.out.println("Call: /server/putset ; Response: "+ key.getEntity());
		
		//Test #3 [GETSET]
		Response set = target.path("/server/getset").request().header("key", "mykey").get();
		System.out.println("Call: /server/getset ; Response: "+ set.getEntity());
		
		//Test #4 [READELEM]
		Response elem = target.path("/server/readelem").request().header("key","mykey").header("pos", "1").get();
		System.out.println("Call: /server/readelem ; Response: " + elem.getEntity());
		
		//Test #5 [ISELEM]
		Response iselem = target.path("/server/iselem").queryParam("elem", "two").request().header("key","mykey").get();
		System.out.println("Call: /server/iselem ; Response: " + iselem.getEntity());
					
		
	}

	static public class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
}
