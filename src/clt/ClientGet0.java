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
import java.util.ArrayList;
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
	
		//the server will return the entry configuration so the client can insert entries
		Future<Response> fut = target.path("/server")
				.request().async().get();
		String configString = fut.get().readEntity(String.class);
		
		//process the SERVER onfiguration returned from the server
		String split[] = configString.split("#"); //[types]#[ops]#Encrypted:[true/false]
		String types[] = split[0].split(" "); //[types]
		String ops[] = split[1].split(" ");
		String isEncrypted[] = split[2].split(":"); //Encrypted:[true/false]
		Object allowedTypes[] = new Object[types.length];
		for(int i = 0 ; i < types.length ; i++){
			if(types[i].equalsIgnoreCase("int"))
				allowedTypes[i] = new Integer(0);
			else
				allowedTypes[i] = new String();
		}
		boolean activeEncryption = false;
		if(isEncrypted[1].equalsIgnoreCase("true"))
			activeEncryption = true;
		Benchmarks test = new Benchmarks(target,activeEncryption); // for the benchmarks
		
		configString = configString.replace("#", " ");
		configString ="Valid Entry: ("+ split[0] + ") Allowed Ops: (" + split[1] + ") " + split[2]; 
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean run = true;
		while(run){
			System.out.println(configString+"\n[0] Sair\n[1] PutSet\n[2] Getset\n[3] AddElem\n[4] RemoveSet\n[5] WriteElem\n[6]"
					+ " ReadElem\n[7] isElem\n[8] Extensive API\n[9] Benchmarks");
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
						n.addCustomElem(new BigInteger(parts[i])); 
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
				System.out.println("[key] [val]");
				String resIselem = br.readLine();
				String partsIselem[] = resIselem.split(" ");
				JSONObject json = new JSONObject();
				json.append("element", partsIselem[1]);
				Future<Response> iselem = target.path("/server/iselem").request()
						.header("key",partsIselem[0]).async().post(Entity.entity(json.toString(),MediaType.APPLICATION_JSON));
				System.out.println("Call: /server/iselem ; Response: " + iselem.get().readEntity(String.class));
				break;
			case "8":
				
				boolean extensiveRun = true;
				
				while(extensiveRun){
					System.out.println("Valid Entry: ("+configString+"\n[0] Go Back\n[1] Sum\n[2] SumAll\n[3] Mult\n[4] MultAll\n[5]"
							+ " SearchEq\n[6] SearchNEq\n[7] SearchEntry\n[8] SearchEntryOR\n[9] SearchEntryAND\n[10]"
							+ " OrderLS\n[11] OrderSL\n[12] SearchEqInt\n[13] SearchGt\n[14] SearchGtEq\n[15] SearchLt\n[16] SearchLtEq");
					String w = br.readLine();
					
					switch(w){
					
					case "0":
						extensiveRun =false;
						break;
					case "1":
						System.out.println("[key1] [key2] [pos]");
						String resSum = br.readLine();
						String partsSum[] = resSum.split(" ");
						int posSum = Integer.parseInt(partsSum[2]);
						if(ops[posSum].equals("+")){
							Future<Response> sum = target.path("/server/sum").request().header("keyOne",partsSum[0]).header("keyTwo", partsSum[1]).header("pos", posSum).async().get();
							System.out.println("Call: /server/sum ; Response: " + sum.get().readEntity(String.class));
						}else{
							System.err.println("Sum not allowed in position: "+posSum+ ".  Allowed operation on this position:  "+ops[posSum]);
						}
						break;
					case "2":
						System.out.println("[pos]");
						int posSumall = Integer.parseInt(br.readLine());
						if(ops[posSumall].equals("+")){
							Future<Response> sumall = target.path("/server/sumall").request().header("pos", posSumall).async().get();
							System.out.println("Call: /server/sumall ; Response: " + sumall.get().readEntity(String.class));
						}else{
							System.err.println("Sumall not allowed in position: "+posSumall+ ".  Allowed operation on this position:  "+ops[posSumall]);
						}
						break;
					case "3":
						System.out.println("[key1] [key2] [pos]");
						String resMult = br.readLine();
						String partsMult[] = resMult.split(" ");
						int posMult = Integer.parseInt(partsMult[2]);
						if(ops[posMult].equals("&")){
						Future<Response> mult = target.path("/server/mult").request().header("keyOne",partsMult[0]).header("keyTwo", partsMult[1]).header("pos", posMult).async().get();
						System.out.println("Call: /server/mult ; Response: " + mult.get().readEntity(String.class));
						}else{
							System.err.println("Mult not allowed in position: "+posMult+ ".  Allowed operation on this position: "+ops[posMult]);
						}break;
					case "4":
						System.out.println("[pos]");
						int posMultall = Integer.parseInt(br.readLine());
						if(ops[posMultall].equals("&")){
							Future<Response> multall = target.path("/server/multall").request().header("pos", posMultall).async().get();
							System.out.println("Call: /server/multall ; Response: " + multall.get().readEntity(String.class));
						}else{
							System.err.println("Multall not allowed in position: "+posMultall+ ".  Allowed operation on this position: "+ops[posMultall]);
						}break;
					case "5":
						System.out.println("[pos] [val]");
						String resSearcheq = br.readLine();
						String partsSearcheq[] = resSearcheq.split(" ");
						int posSearcheq = Integer.parseInt(partsSearcheq[0]);
						if(ops[posSearcheq].equals("=")){
							JSONObject jsobj = new JSONObject();
							jsobj.append("element", partsSearcheq[1]);
							Future<Response> searceq = target.path("/server/searcheq").request().header("pos", posSearcheq).async().post(Entity.entity(jsobj.toString(),MediaType.APPLICATION_JSON));
							List<Entry> seqlist = searceq.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlist)
								System.out.println("Call: /server/searcheq ; Response: " + seqentry.toString());
						}else{
							System.err.println("Searcheq not allowed in position: "+posSearcheq+ ".  Allowed operation on this position: "+ops[posSearcheq]);
						}
						
						break;
					case "6":
						System.out.println("[pos] [val]");
						String resSearchneq = br.readLine();
						String partsSearchneq[] = resSearchneq.split(" ");
						int posSearchneq = Integer.parseInt(partsSearchneq[0]);
						if(ops[posSearchneq].equals("=")){
						JSONObject jsobj2 = new JSONObject();
						jsobj2.append("element", partsSearchneq[1]);
						Future<Response> searchneq = target.path("/server/searchneq").request().header("pos", posSearchneq).async().post(Entity.entity(jsobj2.toString(),MediaType.APPLICATION_JSON));
						List<Entry> seqlist2 = searchneq.get().readEntity(new GenericType<List<Entry>>(){});
						for(Entry seqentry : seqlist2)
							System.out.println("Call: /server/searchneq ; Response: " + seqentry.toString());
						}else{
							System.err.println("Searchneq not allowed in position: "+posSearchneq+ ".  Allowed operation on this position: "+ops[posSearchneq]);
						}break;
					case "7":
						System.out.println("[entry val 1] [entry val 2] ... [entry val "+allowedTypes.length+"]");
						String res7 = br.readLine();
						String[] parts7 = res7.split(" ");
						Entry n7 = new Entry();
						for(int i = 0; i < parts7.length; i++){
							if(allowedTypes[i] instanceof Integer)
								n7.addCustomElem(null); 
							else
								n7.addCustomElem(parts7[i]);
						}
						Future<Response> searchentry = target.path("/server/searchentry").request().async().post(Entity.entity(n7,MediaType.APPLICATION_JSON));
						List<Entry> seqlist3 = searchentry.get().readEntity(new GenericType<List<Entry>>(){});
						for(Entry seqentry : seqlist3)
							System.out.println("Call: /server/searchentry ; Response: " + seqentry.toString());
						
						break;
					case "8":
						System.out.println("[number of entries]");
						int nrEntries = Integer.parseInt(br.readLine());
						List<Entry> entries = new ArrayList<Entry>();
						for(int i = 0; i < nrEntries; i++){
							Entry entryOR = new Entry();
							System.out.println("(Entry "+ (i+1) +"/"+nrEntries+ ") [entry val 1] [entry val 2] ... [entry val "+allowedTypes.length+"]");
							String entryLine = br.readLine();
							String[] entryParts = entryLine.split(" ");
							for(int j = 0 ; j < entryParts.length ; j ++){
								if(allowedTypes[j] instanceof Integer)
									entryOR.addCustomElem(Integer.parseInt(entryParts[j])); 
								else
									entryOR.addCustomElem(entryParts[j]);
							}
							entries.add(entryOR);
						}
						Future<Response> searchentryor = target.path("/server/searchentryor").request().async().post(Entity.entity(entries,MediaType.APPLICATION_JSON));
						List<Entry> seqlist4 = searchentryor.get().readEntity(new GenericType<List<Entry>>(){});
						for(Entry seqentry : seqlist4)
							System.out.println("Call: /server/searchentryor ; Response: " + seqentry.toString());
						break;
					case "9":
						System.out.println("[number of entries]");
						int nrEntriesAnd = Integer.parseInt(br.readLine());
						List<Entry> entriesAnd = new ArrayList<Entry>();
						for(int i = 0; i < nrEntriesAnd; i++){
							Entry entryAND = new Entry();
							System.out.println("(Entry "+ (i+1) +"/"+nrEntriesAnd+ ") [entry val 1] [entry val 2] ... [entry val "+allowedTypes.length+"]");
							String entryLine = br.readLine();
							String[] entryParts = entryLine.split(" ");
							for(int j = 0 ; j < entryParts.length ; j ++){
								if(allowedTypes[j] instanceof Integer)
									entryAND.addCustomElem(Integer.parseInt(entryParts[j])); 
								else
									entryAND.addCustomElem(entryParts[j]);
							}
							entriesAnd.add(entryAND);
						}
						Future<Response> searchentryand = target.path("/server/searchentryand").request().async().post(Entity.entity(entriesAnd,MediaType.APPLICATION_JSON));
						List<Entry> seqlist5 = searchentryand.get().readEntity(new GenericType<List<Entry>>(){});
						for(Entry seqentry : seqlist5)
							System.out.println("Call: /server/searchentryor ; Response: " + seqentry.toString());
						break;
					case "10":
						System.out.println("[pos]");
						int posOrderls = Integer.parseInt(br.readLine());
						if(ops[posOrderls].equals("<") || ops[posOrderls].equals("<=") || ops[posOrderls].equals(">") || ops[posOrderls].equals(">=")){
							Future<Response> orderls = target.path("/server/orderls").request().header("pos", posOrderls).async().get();
							List<Entry> seqlist6 = orderls.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlist6)
								System.out.println("Call: /server/orderls ; Response: " + seqentry.toString());
						}else{
							System.err.println("Orderls not allowed in position: "+posOrderls+ ".  Allowed operation on this position: "+ops[posOrderls]);
						}break;
					case "11":
						System.out.println("[pos]");
						int posOrdersl = Integer.parseInt(br.readLine());
						if(ops[posOrdersl].equals("<") || ops[posOrdersl].equals("<=") || ops[posOrdersl].equals(">") || ops[posOrdersl].equals(">=")){
							Future<Response> ordersl = target.path("/server/ordersl").request().header("pos", posOrdersl).async().get();
							List<Entry> seqlist7 = ordersl.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlist7)
								System.out.println("Call: /server/ordersl ; Response: " + seqentry.toString());
						}else{
							System.err.println("Ordersl not allowed in position: "+posOrdersl+ ".  Allowed operation on this position: "+ops[posOrdersl]);
						}break;
					case "12":
						System.out.println("[pos] [val]");
						String resSearcheqint = br.readLine();
						String partsSearcheqint[] = resSearcheqint.split(" ");
						int posSearcheqint = Integer.parseInt(partsSearcheqint[0]);
						if(ops[posSearcheqint].equals("<") || ops[posSearcheqint].equals("<=") || ops[posSearcheqint].equals(">") || ops[posSearcheqint].equals(">=")){
							JSONObject jsoneqint = new JSONObject();
							jsoneqint.append("element", partsSearcheqint[1]);
							Future<Response> searcheqint = target.path("/server/searcheqint").request().header("pos", posSearcheqint).async().post(Entity.entity(jsoneqint.toString(),MediaType.APPLICATION_JSON));
							List<Entry> seqlisteqint = searcheqint.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlisteqint)
								System.out.println("Call: /server/searcheqint ; Response: " + seqentry.toString());
					}else{
						System.err.println("Searcheqint not allowed in position: "+posSearcheqint+ ".  Allowed operation on this position: "+ops[posSearcheqint]);
					}break;
					case "13":
						System.out.println("[pos] [val]");
						String resSearchgt = br.readLine();
						String partsSearchgt[] = resSearchgt.split(" ");
						int posSearchgt = Integer.parseInt(partsSearchgt[0]);
						if(ops[posSearchgt].equals("<") || ops[posSearchgt].equals("<=") || ops[posSearchgt].equals(">") || ops[posSearchgt].equals(">=")){
							JSONObject jsongt = new JSONObject();
							jsongt.append("element", partsSearchgt[1]);
							Future<Response> searchgt = target.path("/server/searchgt").request().header("pos", posSearchgt).async().post(Entity.entity(jsongt.toString(),MediaType.APPLICATION_JSON));
							List<Entry> seqlistgt = searchgt.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlistgt)
								System.out.println("Call: /server/searchgt ; Response: " + seqentry.toString());
					}else{
						System.err.println("Searchgt not allowed in position: "+posSearchgt+ ".  Allowed operation on this position: "+ops[posSearchgt]);
					}break;
					case "14":
						System.out.println("[pos] [val]");
						String resSearchgteq = br.readLine();
						String partsSearchgteq[] = resSearchgteq.split(" ");
						int posSearchgteq = Integer.parseInt(partsSearchgteq[0]);
						if(ops[posSearchgteq].equals("<") || ops[posSearchgteq].equals("<=") || ops[posSearchgteq].equals(">") || ops[posSearchgteq].equals(">=")){
							JSONObject jsongteq = new JSONObject();
							jsongteq.append("element", partsSearchgteq[1]);
							Future<Response> searchgteq = target.path("/server/searchgteq").request().header("pos", posSearchgteq).async().post(Entity.entity(jsongteq.toString(),MediaType.APPLICATION_JSON));
							List<Entry> seqlistgteq = searchgteq.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlistgteq)
								System.out.println("Call: /server/searchgteq ; Response: " + seqentry.toString());
						}else{
							System.err.println("Searchgteq not allowed in position: "+posSearchgteq+ ".  Allowed operation on this position: "+ops[posSearchgteq]);
						}break;
					case "15":
						System.out.println("[pos] [val]");
						String resSearchlt = br.readLine();
						String partsSearchlt[] = resSearchlt.split(" ");
						int posSearchlt = Integer.parseInt(partsSearchlt[0]);
							if(ops[posSearchlt].equals("<") || ops[posSearchlt].equals("<=") || ops[posSearchlt].equals(">") || ops[posSearchlt].equals(">=")){
							JSONObject jsonlt = new JSONObject();
							jsonlt.append("element", partsSearchlt[1]);
							Future<Response> searchlt = target.path("/server/searchlt").request().header("pos", posSearchlt).async().post(Entity.entity(jsonlt.toString(),MediaType.APPLICATION_JSON));
							List<Entry> seqlistlt = searchlt.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlistlt)
								System.out.println("Call: /server/searchlt ; Response: " + seqentry.toString());
						}else{
							System.err.println("Searchlt not allowed in position: "+posSearchlt+ ".  Allowed operation on this position: "+ops[posSearchlt]);
						}break;
					case "16":
						System.out.println("[pos] [val]");
						String resSearchlteq = br.readLine();
						String partsSearchlteq[] = resSearchlteq.split(" ");
						int posSearchlteq = Integer.parseInt(partsSearchlteq[0]);
						if(ops[posSearchlteq].equals("<") || ops[posSearchlteq].equals("<=") || ops[posSearchlteq].equals(">") || ops[posSearchlteq].equals(">=")){
							JSONObject jsonlteq = new JSONObject();
							jsonlteq.append("element", partsSearchlteq[1]);
							Future<Response> searchlteq = target.path("/server/searchlteq").request().header("pos", posSearchlteq).async().post(Entity.entity(jsonlteq.toString(),MediaType.APPLICATION_JSON));
							List<Entry> seqlistlteq = searchlteq.get().readEntity(new GenericType<List<Entry>>(){});
							for(Entry seqentry : seqlistlteq)
								System.out.println("Call: /server/searchlteq ; Response: " + seqentry.toString());
					}else{
							System.err.println("Searchlteq not allowed in position: "+posSearchlteq+ ".  Allowed operation on this position: "+ops[posSearchlteq]);
						}break;
					}
				}
				break;
			case "9"://Test #8 [BENCHMARKS]
				String m ;
				boolean benchRun = true;
				while(benchRun){
					System.out.println("[0] Go Back\n[1] Benchmark1\n[2] Benchmark2\n[3] Benchmark3"+
							"\n[4] Benchmark4\n[5] Benchmark5\n[6] Benchmark E1/E3\n[7] Benchmark E2/E4");
					m = br.readLine();
					switch(m){
					case "0": benchRun = false; break;
					case "1":	test.benchmark1(); break;
					case "2":	test.benchmark2(); break;
					case "3":	test.benchmark3(); break;
					case "4":    test.benchmark4(); break;
					case "5":	test.benchmark5(); break;
					case "6":    test.benchmarkE1E3(); break;
					case "7":    test.benchmarkE2E4(); break;
					}
				}
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
