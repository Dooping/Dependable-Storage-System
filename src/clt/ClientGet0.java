package clt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
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
import javax.ws.rs.core.GenericType;
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
import org.json.JSONObject;

import Datatypes.Entry;
import tests.Benchmarks;

public class ClientGet0 {
	static final File KEYSTORE = new File("./home/csd/client.jks");
	static final char[] JKS_PASSWORD = "changeit".toCharArray();
	static final char[] KEY_PASSWORD = "changeit".toCharArray();

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		
		Option hostnameOp = new Option("h", "hostname", true, "rest server address");
		hostnameOp.setRequired(false);
        options.addOption(hostnameOp);
        
        Option keystoreOp = new Option("k", "keystore", true, "truststore path");
        keystoreOp.setRequired(false);
        options.addOption(keystoreOp);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ClientGet0", options);

            System.exit(1);
            return;
        }
		
		String hostname = cmd.getOptionValue("hostname", "localhost:9090");
		File keystore = KEYSTORE;
		if( cmd.hasOption("keystore"))
			keystore = new File(cmd.getOptionValue("keystore"));

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
        
		Client client = ClientBuilder.newBuilder()
				.hostnameVerifier(new InsecureHostnameVerifier())
				.sslContext(sslContext)
				.build();

		URI baseURI = UriBuilder.fromUri("https://" + hostname + "/").build();
		WebTarget target = client.target(baseURI);

		Benchmarks test = new Benchmarks(target); // for the benchmarks
		
		//the server will return the entry configuration so the client can insert entries
		Future<Response> fut = target.path("/server")
				.request().async().get();
		String configString = fut.get().readEntity(String.class);
		
		//process the Entry configuration returned from the server
		String split[] = configString.split("\n"); //int string int string int string/\n& $ # " !
		String types[] = split[0].split(" "); //int string int string int string
		Object allowedTypes[] = new Object[types.length];
		for(int i = 0 ; i < types.length ; i++){
			if(types[i].equalsIgnoreCase("int"))
				allowedTypes[i] = new Integer(0);
			else
				allowedTypes[i] = new String();
		}
		
		configString = configString.replace("\n", ") Allowed Ops: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean run = true;
		while(run){
			System.out.println("Valid Entry: ("+configString+"\n[0] Sair\n[1] PutSet\n[2] Getset\n[3] AddElem\n[4] RemoveSet\n[5] WriteElem\n[6]"
					+ " ReadElem\n[7] isElem\n[8] Extensive API\n[9] Benchmark1\n[10] Benchmark2\n[11]"
					+ " Benchmark3\n[12] Benchmark4\n[13] Benchmark5");
	        String s = br.readLine();
			switch(s){
			case "0": run = false;
					break;
			case "1"://Test #1 [PUTSET]
				System.out.println("[key] [entry val 1] [entry val 2] ... [entry val "+allowedTypes.length+"]");
				String res = br.readLine();
				String[] parts = res.split(" ");
				Entry n = new Entry();
				for(int i = 1; i < parts.length; i++){
					if(allowedTypes[i-1] instanceof Integer)//since i starts at 1, use i-1 to go get the first index of allowedTypes
						n.addCustomElem(Integer.parseInt(parts[i])); 
					else
						n.addCustomElem(parts[i]); 
				}
				
				Future<Response> key = target.path("/server/putset")
						.request().header("key", parts[0]).async().post(Entity.entity(n, MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/putset ; Response: "+ key.get().readEntity(Long.class));
				break;
			case "2"://Test #2 [GETSET]
				System.out.println("[key]");
				String key2 = br.readLine();
				Future<Response> set = target.path("/server/getset").request().header("key",key2).async().get();
				System.out.println("Call: /server/getset ; Response: "+ set.get().readEntity(Entry.class));
				break;
			case "3"://test #3 [ADDELEM]
				System.out.println("[key]");
				String key3 = br.readLine();
				Future<Response> addelem = target.path("/server/addelem").request()
					.header("key", key3).async().post(Entity.entity("", MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/addelem ; Response: "+addelem.get().readEntity(Long.class));
				break;
			case "4"://test #4 [REMOVESET]
				System.out.println("[key]");
				String key4 = br.readLine();
				Future<Response> delete = target.path("/server/removeset").request().header("key", key4).async().delete();
				System.out.println("Call: /server/removeset ; Response: "+delete.get().readEntity(Long.class));
				break;
			case "5"://test #5 [WRITEELEM]
				System.out.println("[key] [pos] [new val]");
				String res5 = br.readLine();
				String parts5[] = res5.split(" ");
				int pos = Integer.parseInt(parts5[1]);
				JSONObject jsonobj = new JSONObject();
				if(allowedTypes[pos] instanceof String){
					jsonobj.append("element", parts5[2]);
				}else{
					jsonobj.append("element",Integer.parseInt(parts5[2]));
				}
				Future<Response> val = target.path("/server/writeelem").request()
						.header("key", parts5[0]).header("pos", pos).async().post(Entity.entity(jsonobj.toString(), MediaType.APPLICATION_JSON));
				System.out.println(val.toString());
				break;
			case "6"://Test #6 [READELEM]
				System.out.println("[key] [pos]");
				String res6 = br.readLine();
				String parts6[] = res6.split(" ");
				int pos6 = Integer.parseInt(parts6[1]);
				Future<Response> elem = target.path("/server/readelem").request().header("key",parts6[0]).header("pos", pos6).async().get();
				System.out.println("Call: /server/readelem ; Response: " + elem.get().readEntity(String.class));
				break;
			case "7"://Test #7 [ISELEM]
				JSONObject json = new JSONObject();
				json.append("element", "two");
				Future<Response> iselem = target.path("/server/iselem").request()
						.header("key","mykey").async().post(Entity.entity(json.toString(),MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/iselem ; Response: " + iselem.get().readEntity(String.class));
				break;
			case "8":
				
				boolean extensiveRun = true;
				
				while(extensiveRun){
					System.out.println("[0] Go Back\n[1] Sum\n[2] SumAll\n[3] Mult\n[4] MultAll\n[5]"
							+ " SearchEq\n[6] SearchNEq\n[7] SearchEntry\n[8] SearchEntryOR\n[9] SearchEntryAND\n[10]"
							+ " OrderLS\n[11] OrderSL\n[12] SearchGt\n[13] SearchGtEq\n[14] SearchLt\n[15] SearchLtEq");
					String w = br.readLine();
					
					switch(w){
					
					case "0":
						extensiveRun =false;
						break;
					case "1":
						Future<Response> sum = target.path("/server/sum").request().header("keyOne","mykey").header("keyTwo", "mykey").header("pos", "2").async().get();
						System.out.println("Call: /server/sum ; Response: " + sum.get().readEntity(String.class));
						break;
					case "2":
						Future<Response> sumall = target.path("/server/sumall").request().header("pos", "2").async().get();
						System.out.println("Call: /server/sumall ; Response: " + sumall.get().readEntity(String.class));
						break;
					case "3":
						Future<Response> mult = target.path("/server/mult").request().header("keyOne","mykey").header("keyTwo", "mykey").header("pos", "4").async().get();
						System.out.println("Call: /server/mult ; Response: " + mult.get().readEntity(String.class));
						break;
					case "4":
						Future<Response> multall = target.path("/server/multall").request().header("pos", "4").async().get();
						System.out.println("Call: /server/multall ; Response: " + multall.get().readEntity(String.class));
						break;
					case "5":
						JSONObject jsobj = new JSONObject();
						jsobj.append("element", "four");
						Future<Response> seq = target.path("/server/searcheq").request().header("pos", "3").async().post(Entity.entity(jsobj.toString(),MediaType.APPLICATION_JSON));
						List<Entry> seqlist;
						seqlist = seq.get().readEntity(new GenericType<List<Entry>>(){});
						for(Entry seqentry : seqlist)
							System.out.println("Call: /server/searcheq ; Response: " + seqentry.toString());
						break;
					case "6":
						break;
					case "7":
						break;
					case "8":
						break;
					case "9":
						break;
					case "10":
						break;
					case "11":
						break;
					case "12":
						break;
					case "13":
						break;
					case "14":
						break;
					case "15":
						break;
					case "16":
						break;
					}
					
					
				}
				break;
			case "9"://Test #8 [BENCHMARK1]
				test.benchmark1();
				break;
			case "10"://Test #9 [BENCHMARK2]
				test.benchmark2();
				break;
			case "11"://Test #10 [BENCHMARK3]
				test.benchmark3();
				break;
			case "12"://Test #11 [BENCHMARK4]
				test.benchmark4();
				break;
			case "13"://Test #12 [BENCHMARK5]
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
