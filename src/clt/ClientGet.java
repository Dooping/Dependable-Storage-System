package clt;

import Datatypes.*;
import tests.Benchmarks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.Future;

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

import org.json.JSONObject;

import javax.ws.rs.client.InvocationCallback;

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
		Benchmarks test = new Benchmarks(target); // for the benchmarks
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean run = true;
		while(run){
			System.out.println("[0] Sair\n[1] PutSet\n[2] Getset\n[3] AddElem\n[4] RemoveSet\n[5] WriteElem\n[6]"
					+ " ReadElem\n[7] isElem\n[8] Benchmark1\n[9] Benchmark2\n[10]"
					+ " Benchmark3\n[11] Benchmark4\n[12] Benchmark5");
	        String s = br.readLine();
			switch(s){
			case "0": run = false;
					break;
			case "1"://Test #1 [PUTSET]
				Future<Response> key = target.path("/server/putset")
						.request().header("key", "mykey").async().post(Entity.entity(new Entry(1,"two",3,"four",5,"six"), MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/putset ; Response: "+ key.get().readEntity(Long.class));
				break;
			case "2"://Test #2 [GETSET]
				Future<Response> set = target.path("/server/getset").request().header("key", "mykey").async().get();
				System.out.println("Call: /server/getset ; Response: "+ set.get().readEntity(Entry.class));
				break;
			case "3"://test #3 [ADDELEM]
				Future<Response> addelem = target.path("/server/addelem").request()
					.header("key", "mykey").async().post(Entity.entity("", MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/addelem ; Response: "+addelem.get().readEntity(Long.class));
				break;
			case "4"://test #4 [REMOVESET]
				Future<Response> delete = target.path("/server/removeset").request().header("key", "mykey").async().delete();
				System.out.println("Call: /server/removeset ; Response: "+delete.get().readEntity(Long.class));
				break;
			case "5"://test #5 [WRITEELEM]
				JSONObject jsonobj = new JSONObject();
				jsonobj.append("element", 2);
				Future<Response> val = target.path("/server/writeelem").request()
						.header("key", "mykey").header("pos", 2).async().post(Entity.entity(jsonobj.toString(), MediaType.APPLICATION_JSON));
				System.out.println(val.toString());
				break;
			case "6"://Test #6 [READELEM]
				Future<Response> elem = target.path("/server/readelem").request().header("key","mykey").header("pos", "1").async().get();
				System.out.println("Call: /server/readelem ; Response: " + elem.get().readEntity(String.class));
				break;
			case "7"://Test #7 [ISELEM]
				JSONObject json = new JSONObject();
				json.append("element", "two");
				Future<Response> iselem = target.path("/server/iselem").request()
						.header("key","mykey").async().post(Entity.entity(json.toString(),MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/iselem ; Response: " + iselem.get().readEntity(String.class));
				break;
			case "8"://Test #8 [BENCHMARK1]
				test.benchmark1();
				break;
			case "9"://Test #9 [BENCHMARK2]
				test.benchmark2();
				break;
			case "10"://Test #10 [BENCHMARK3]
				test.benchmark3();
				break;
			case "11"://Test #11 [BENCHMARK4]
				test.benchmark4();
				break;
			case "12"://Test #12 [BENCHMARK5]
				test.benchmark5();
				break;
			}
			
		}

	}

	static public class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
}
