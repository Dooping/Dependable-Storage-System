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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.ws.rs.client.InvocationCallback;

public class ClientGet {

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		
		Option hostnameOp = new Option("h", "hostname", true, "hostname adress");
		hostnameOp.setRequired(false);
        options.addOption(hostnameOp);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ClientGet", options);

            System.exit(1);
            return;
        }
		
		String hostname = cmd.getOptionValue("hostname", "localhost:9090");

		Client client = ClientBuilder.newBuilder()
				.hostnameVerifier(new InsecureHostnameVerifier())
				.build();

		URI baseURI = UriBuilder.fromUri("http://" + hostname + "/").build();
		WebTarget target = client.target(baseURI);
		Benchmarks test = new Benchmarks(target); // for the benchmarks
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean run = true;
		while(run){
			System.out.println("[0] Sair\n[1] Get\n[2] Putset\n[3] Getset\n[4]"
					+ " ReadElem\n[5] isElem\n[6] Benchmark1\n[7] Benchmark2\n[8]"
					+ " Benchmark3\n[9] Benchmark4\n[10] Benchmark5");
	        String s = br.readLine();
			switch(s){
			case "0": run = false;
					break;
			case "1"://Test #1 [ASYNC]
				final Future<Entry> value = target.path("/server")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.async()
					.get(Entry.class);
				System.out.println(value.get());
				
				break;
			case "2"://Test #2 [PUTSET]
				Future<Response> key = target.path("/server/putset")
						.request().header("key", "mykey").async().post(Entity.entity(new Entry(1,"2",3,"4",5,"6"), MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/putset ; Response: "+ key.get().readEntity(Long.class));
				break;
			case "3"://Test #3 [GETSET]
				Future<Response> set = target.path("/server/getset").request().header("key", "mykey").async().get();
				System.out.println("Call: /server/getset ; Response: "+ set.get().readEntity(Entry.class));
				break;
			case "4"://Test #4 [READELEM]
				Response elem = target.path("/server/readelem").request().header("key","mykey").header("pos", "1").get();
				System.out.println("Call: /server/readelem ; Response: " + elem.getEntity());
				break;
			case "5"://Test #5 [ISELEM]
				Response iselem = target.path("/server/iselem").queryParam("elem", "two").request().header("key","mykey").get();
				System.out.println("Call: /server/iselem ; Response: " + iselem.getEntity());
				break;
			case "6"://Test #6 [BENCHMARK1]
				test.benchmark1();
				break;
			case "7"://Test #7 [BENCHMARK2]
				test.benchmark2();
				break;
			case "8"://Test #7 [BENCHMARK3]
				test.benchmark3();
				break;
			case "9"://Test #7 [BENCHMARK4]
				test.benchmark4();
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
