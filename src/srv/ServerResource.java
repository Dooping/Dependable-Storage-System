package srv;

import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import Datatypes.*;
import auxiliary.*;
import akka.actor.ActorSelection;
//import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import auxiliary.EntryConfig;
import hlib.hj.mlib.HomoAdd;
import hlib.hj.mlib.HomoMult;
import hlib.hj.mlib.PaillierKey;

import org.glassfish.jersey.server.ManagedAsync;


import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


import org.json.*;

@Path("/server")
public class ServerResource {

	@Context ActorSystem actorSystem;
	String value = "default";
	Entry dummyEntry = new Entry(1,"two",3,"four",5,"six");
	EntryConfig conf;
	private boolean activeEncryption;
	
	public ServerResource(){
		conf = new EntryConfig(EntryConfig.CONF_FILE);
		activeEncryption = true; //set as true for testing
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ManagedAsync
	public void get(@Suspended final AsyncResponse asyncResponse){
		asyncResponse.resume(Response.ok().entity(conf.getConfigString()).build());
	}
	
	@POST
	@Path("/putset")
	@Consumes(MediaType.APPLICATION_JSON)
	@ManagedAsync
	public void putset(@HeaderParam("key") String key, Entry entry, @Suspended final AsyncResponse asyncResponse){
		
		Entry crypt = conf.encryptEntry(entry);
		
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		APIWrite write = new APIWrite(System.nanoTime(), key,"clientidip",crypt);
		Future<Object> future = Patterns.ask(proxy, write, timeout);
		future.onComplete(new OnComplete<Object>() {

            public void onComplete(Throwable failure, Object result) {
            	
            	if(failure != null){
            		if(failure.getMessage() != null)
            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
            		else
            			asyncResponse.resume(Response.serverError());
            	}else{
            		long res = (long)result;
            		asyncResponse.resume(Response.ok().entity(res).build());
            	}
            }
        }, actorSystem.dispatcher());
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getset")
	public void getSet(@HeaderParam("key") String key, @Suspended final AsyncResponse asyncResponse){	
		
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		Read read = new Read(System.nanoTime(),key);
		Future<Object> future = Patterns.ask(proxy, read, timeout);
		future.onComplete(new OnComplete<Object>() {
            public void onComplete(Throwable failure, Object result) {
            	if(failure != null){
            		if(failure.getMessage() != null)
            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
            		else
            			asyncResponse.resume(Response.serverError());
            	}else{
            		ReadResult res = (ReadResult)result;
            		asyncResponse.resume(Response.ok().entity(conf.decryptEntry(res.v())).build());
            	}
            }
        }, actorSystem.dispatcher());
		
	}
	
	@POST
	@Path("/addelem")
	public void addElem(@HeaderParam("key") String key,@Suspended final AsyncResponse asyncResponse){
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		
		Read read = new Read(System.nanoTime(),key);
		Future<Object> future = Patterns.ask(proxy, read, timeout);
		future.onComplete(new OnComplete<Object>() {

            public void onComplete(Throwable failure, Object result) {
            	
            	if(failure != null){
            		if(failure.getMessage() != null)
            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
            		else
            			asyncResponse.resume(Response.serverError());
            	}else{
            		ReadResult res = (ReadResult)result;
            		Entry entry = res.v();
            		if (entry == null)
            			entry = new Entry();
            		entry.addElem();
            		APIWrite write = new APIWrite(System.nanoTime(), key,"clientidip",entry);
            		Future<Object> future = Patterns.ask(proxy, write, timeout);
            		future.onComplete(new OnComplete<Object>() {

                        public void onComplete(Throwable failure, Object result) {
                        	if(failure != null){
                        		if(failure.getMessage() != null)
                        			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
                        		else
                        			asyncResponse.resume(Response.serverError());
                        	}else{
                        		long res = (long)result;
                        		asyncResponse.resume(Response.ok().entity(res).build());
                        	}
                        }
                    }, actorSystem.dispatcher());
            		
            	}
            }
        }, actorSystem.dispatcher());
	}
	
	@DELETE
	@Path("/removeset")
	public void removeSet(@HeaderParam("key") String key,@Suspended final AsyncResponse asyncResponse){
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		
		APIWrite write = new APIWrite(System.nanoTime(), key,"clientidip",null);
		Future<Object> future = Patterns.ask(proxy, write, timeout);
		future.onComplete(new OnComplete<Object>() {

            public void onComplete(Throwable failure, Object result) {
            	
            	if(failure != null){
            		if(failure.getMessage() != null)
            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
            		else
            			asyncResponse.resume(Response.serverError());
            	}else{
            		long res = (long)result;
            		asyncResponse.resume(Response.ok().entity(res).build());
            	}
            }
        }, actorSystem.dispatcher());
	}
	
	@POST
	@Path("/writeelem")
	@Consumes(MediaType.APPLICATION_JSON)
	public void writeElem(@HeaderParam("key") String key, String json,@HeaderParam("pos") int pos,@Suspended final AsyncResponse asyncResponse){
		JSONObject o = new JSONObject(json);
		JSONArray jdata = o.getJSONArray("element");
		Object obj = jdata.get(0);
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		
		Read read = new Read(System.nanoTime(),key);
		Future<Object> future = Patterns.ask(proxy, read, timeout);
		future.onComplete(new OnComplete<Object>() {

            public void onComplete(Throwable failure, Object result) {
            	
            	if(failure != null){
            		if(failure.getMessage() != null)
            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
            		else
            			asyncResponse.resume(Response.serverError());
            	}else{
            		ReadResult res = (ReadResult)result;
            		Entry entry = res.v();
            		if(entry == null)
            			asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).entity("Key not found: " + key).build());
            		else if(pos>entry.values.size())
            			asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).entity("Position not valid: " + pos).build());
            		else{
	            		entry.values.remove(pos);
	            		entry.values.add(pos,conf.encryptElem(pos, obj));
	            		APIWrite write = new APIWrite(System.nanoTime(), key,"clientidip",entry);
	            		Future<Object> future = Patterns.ask(proxy, write, timeout);
	            		future.onComplete(new OnComplete<Object>() {
	
	                        public void onComplete(Throwable failure, Object result) {
	                        	if(failure != null){
	                        		if(failure.getMessage() != null)
	                        			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	                        		else
	                        			asyncResponse.resume(Response.serverError());
	                        	}else{
	                        		long res = (long)result;
	                        		asyncResponse.resume(Response.ok().entity(res).build());
	                        	}
	                        }
	                    }, actorSystem.dispatcher());
            		}
            		
            	}
            }
        }, actorSystem.dispatcher());
		
	}
	
	@GET
	@Path("/readelem")
	@Produces(MediaType.APPLICATION_JSON)
	public void readElem(@HeaderParam("key") String key,@HeaderParam("pos") int pos,@Suspended final AsyncResponse asyncResponse){
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		
		Read read = new Read(System.nanoTime(),key);
		Future<Object> future = Patterns.ask(proxy, read, timeout);
		future.onComplete(new OnComplete<Object>() {

            public void onComplete(Throwable failure, Object result) {
            	
            	if(failure != null){
            		if(failure.getMessage() != null)
            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
            		else
            			asyncResponse.resume(Response.serverError());
            	}else{
            		ReadResult res = (ReadResult)result;
            		if(res.v()!=null)
            			asyncResponse.resume(Response.ok().entity(conf.decryptElem(pos, res.v().getElem(pos))).build());
            		else
            			asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).entity("Key not found: " + key).build());
            	}
            }
        }, actorSystem.dispatcher());
	}
	
	@POST
	@Path("/iselem")
	@Produces(MediaType.APPLICATION_JSON)
	public void isElem(@HeaderParam("key")String key, String json, @Suspended final AsyncResponse asyncResponse){
		//In order to check if this element exists in list of values, how do we
		//know if we should compare it with string or int when we have a list of Objects?
		JSONObject o = new JSONObject(json);
		JSONArray jdata = o.getJSONArray("element");
		Object obj = jdata.get(0);
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		
		Read read = new Read(System.nanoTime(),key);
		Future<Object> future = Patterns.ask(proxy, read, timeout);
		future.onComplete(new OnComplete<Object>() {

            public void onComplete(Throwable failure, Object result) {
            	
            	if(failure != null){
            		if(failure.getMessage() != null)
            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
            		else
            			asyncResponse.resume(Response.serverError());
            	}else{
            		ReadResult res = (ReadResult)result;
                	if(res.v()!=null)
                		asyncResponse.resume(Response.ok().entity(res.v().values.contains(obj)).build());
                	else
                		asyncResponse.resume(Response.ok().entity(false).build());
            	}
            }
        }, actorSystem.dispatcher());
	}
	
	
		//=====EXTENSIVE API======
		@GET
		@Path("/sum")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void sum(@HeaderParam("pos") int pos,@HeaderParam("keyOne")String keyOne,@HeaderParam("keyTwo")String keyTwo, @Suspended final AsyncResponse asyncResponse){
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			Read read = new Read(System.nanoTime(),keyOne);
			Future<Object> future = Patterns.ask(proxy, read, timeout);
			future.onComplete(new OnComplete<Object>() {
	            public void onComplete(Throwable failure, Object result) {
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		ReadResult res = (ReadResult)result;
	            		Entry entry1 = res.v();
	            		if(entry1 == null)
	            			asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).entity("Key not found: " + keyOne).build());
	            		else if(pos>entry1.values.size())
	            			asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).entity("Position not valid: " + pos).build());
	            		else{
	            			Read read2 = new Read(System.nanoTime(),keyTwo);
	            			Future<Object> future2 = Patterns.ask(proxy, read2, timeout);
		            		future2.onComplete(new OnComplete<Object>() {
		
		                        public void onComplete(Throwable failure, Object result) {
		                        	if(failure != null){
		                        		if(failure.getMessage() != null)
		                        			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
		                        		else
		                        			asyncResponse.resume(Response.serverError());
		                        	}else{
		                        		try{
		                        		ReadResult res2 = (ReadResult)result;
		        	            		Entry entry2 = res2.v();
		        	            		BigInteger big1Code = (BigInteger) entry1.getElem(pos);
		        	            		BigInteger big2Code = (BigInteger) entry2.getElem(pos); 
		        	            		PaillierKey pk = (PaillierKey)conf.keys.get(pos).getKey(0);
		        	            		BigInteger res = HomoAdd.sum(big1Code, big2Code, pk.getNsquare());
		                        		asyncResponse.resume(Response.ok().entity(HomoAdd.decrypt(res, pk)).build());
		                        		}catch(Exception e){
		                        			e.printStackTrace();
		                        		}
		                        	}
		                        }
		                    }, actorSystem.dispatcher());
	            		}
	            		
	            	}
	            }
	        }, actorSystem.dispatcher());
		}

		@GET
		@Path("/sumall")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void sumAll(@HeaderParam("pos")int pos, @Suspended final AsyncResponse asyncResponse){	
			
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			PaillierKey pkey = (PaillierKey) conf.keys.get(pos).getKey(0);
			SumAll sum = new SumAll(System.nanoTime(),pos,activeEncryption,pkey.getNsquare());
			Future<Object> future = Patterns.ask(proxy, sum, timeout);
			future.onComplete(new OnComplete<Object>() {

	            public void onComplete(Throwable failure, Object result) throws Exception {
	            	
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		SumMultAllResult res = (SumMultAllResult) result;
	                	if(res.res()!=null){
	                		BigInteger big = res.res();
	                		BigInteger truePaiVal = HomoAdd.decrypt(big, pkey);
	                		asyncResponse.resume(Response.ok().entity(truePaiVal.toString()).build());
	                	}
	                	else
	                		asyncResponse.resume(Response.ok().entity(false).build());
	            	
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@GET
		@Path("/mult")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void mult(@HeaderParam("pos") int pos,@HeaderParam("keyOne") String keyOne, @HeaderParam("keyTwo") String keyTwo, @Suspended final AsyncResponse asyncResponse){
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			Read read = new Read(System.nanoTime(),keyOne);
			Future<Object> future = Patterns.ask(proxy, read, timeout);
			future.onComplete(new OnComplete<Object>() {
	            public void onComplete(Throwable failure, Object result) {
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		ReadResult res = (ReadResult)result;
	            		Entry entry1 = res.v();
	            		if(entry1 == null)
	            			asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).entity("Key not found: " + keyOne).build());
	            		else if(pos>entry1.values.size())
	            			asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).entity("Position not valid: " + pos).build());
	            		else{
	            			Read read2 = new Read(System.nanoTime(),keyTwo);
	            			Future<Object> future2 = Patterns.ask(proxy, read2, timeout);
		            		future2.onComplete(new OnComplete<Object>() {
		
		                        public void onComplete(Throwable failure, Object result) {
		                        	if(failure != null){
		                        		if(failure.getMessage() != null)
		                        			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
		                        		else
		                        			asyncResponse.resume(Response.serverError());
		                        	}else{
		                        		try{
			                        		ReadResult res2 = (ReadResult)result;
			        	            		Entry entry2 = res2.v();
			        	            		BigInteger big1Code = (BigInteger) entry1.getElem(pos);
			        	            		BigInteger big2Code = (BigInteger) entry2.getElem(pos); 
			        	            		RSAPublicKey pubkey = (RSAPublicKey)conf.keys.get(pos).getKey(0);
			        	            		RSAPrivateKey prikey = (RSAPrivateKey)conf.keys.get(pos).getKey(1);
			        	            		BigInteger product = HomoMult.multiply(big1Code, big2Code, pubkey);
			                        		asyncResponse.resume(Response.ok().entity(HomoMult.decrypt(prikey,product)).build());
		                        		}catch(Exception e){
		                        			e.printStackTrace();
		                        		}
		                        	}
		                        }
		                    }, actorSystem.dispatcher());
	            		}
	            		
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@GET
		@Path("/multall")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void multAll(@HeaderParam("pos") int pos, @Suspended final AsyncResponse asyncResponse){
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			RSAPublicKey rsapubkey = (RSAPublicKey)conf.keys.get(pos).getKey(0);
			MultAll mult = new MultAll(System.nanoTime(),pos,activeEncryption,rsapubkey);
			Future<Object> future = Patterns.ask(proxy, mult, timeout);
			future.onComplete(new OnComplete<Object>() {

	            public void onComplete(Throwable failure, Object result) throws Exception {
	            	
	            	if(failure != null){
	            		System.out.println("FAILURE");
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		SumMultAllResult res = (SumMultAllResult) result;
	            		System.out.println("REES:"+res);
	                	
	                		BigInteger big = res.res();
	                		RSAPrivateKey rsaprivKey =(RSAPrivateKey) conf.keys.get(pos).getKey(1);
	                		BigInteger trueRSAVal = HomoMult.decrypt(rsaprivKey, big);
	                		System.out.println("[MULTALL]:"+trueRSAVal.toString());
	                		asyncResponse.resume(Response.ok().entity(trueRSAVal.toString()).build());
	                	
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searcheq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			//return Response.ok().build();
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String val =(String) jdata.get(0);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchEq seq = new SearchEq(System.nanoTime(), pos,(String)conf.encryptElem(pos, val));
			Future<Object> future = Patterns.ask(proxy, seq, timeout);
			future.onComplete(new OnComplete<Object>() {

	            public void onComplete(Throwable failure, Object result) throws Exception {
	            	
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		EntrySet res = (EntrySet) result;
	            		List<Entry> seqlist = res.set();
	            		List<Entry> decryptedEntries = new ArrayList<Entry>();
	            		for(Entry n : seqlist){
	            			decryptedEntries.add(conf.decryptEntry(n));
	            		}
	                	if(res.set()!= null){
	                		asyncResponse.resume(Response.ok().entity(decryptedEntries).build());
	                	}
	                	else
	                		asyncResponse.resume(Response.ok().entity(false).build());
	            	
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searchneq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchNEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			//return Response.ok().build();
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String val =(String) jdata.get(0);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchNEq seq = new SearchNEq(System.nanoTime(), pos,(String)conf.encryptElem(pos, val));
			Future<Object> future = Patterns.ask(proxy, seq, timeout);
			future.onComplete(new OnComplete<Object>() {

	            public void onComplete(Throwable failure, Object result) throws Exception {
	            	
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		EntrySet res = (EntrySet) result;
	            		List<Entry> seqlist = res.set();
	            		List<Entry> decryptedEntries = new ArrayList<Entry>();
	            		for(Entry n : seqlist){
	            			decryptedEntries.add(conf.decryptEntry(n));
	            		}
	                	if(res.set()!= null){
	                		asyncResponse.resume(Response.ok().entity(decryptedEntries).build());
	                	}
	                	else
	                		asyncResponse.resume(Response.ok().entity(false).build());
	            	
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searchentry")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchEntry(Entry entry, @Suspended final AsyncResponse asyncResponse){
			Entry specialEntry = new Entry();
			System.out.println("[SEARCHENTRY] " + entry.toString());
			boolean[] searchables = conf.getOpIndex("%");
			
			List<Object> values = entry.values;
			for(int i = 0 ; i < values.size() ; i ++){
				if(values.get(i)!=null && searchables[i]){ //se for diferente de null e for um campo de % (search)
					if(activeEncryption)
						specialEntry.addCustomElem(conf.encryptElem(i, values.get(i)));
					else
						specialEntry.addCustomElem(values.get(i));
				}else{
					specialEntry.addCustomElem(null);
				}
			}
			
				
			System.out.println("[SEARCHENTRY] " + entry.toString());
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(5, "seconds"));
			SearchEntry sent = new SearchEntry(System.nanoTime(),specialEntry,activeEncryption);
			Future<Object> future = Patterns.ask(proxy, sent, timeout);
			future.onComplete(new OnComplete<Object>() {

	            public void onComplete(Throwable failure, Object result) throws Exception {
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		EntrySet res = (EntrySet) result;
	            		List<Entry> seqlist = res.set();
	            		System.out.println("[SEARCHENTRY] " + res.toString());
	            		List<Entry> decryptedEntries = new ArrayList<Entry>();
	            		for(Entry n : seqlist){
	            			decryptedEntries.add(conf.decryptEntry(n));
	            		}
	                	if(res.set()!= null){
	                		asyncResponse.resume(Response.ok().entity(decryptedEntries).build());
	                	}
	                	else
	                		asyncResponse.resume(Response.ok().entity(false).build());
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		
		@POST
		@Path("/searchentryor")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchEntryOR(List<Entry> entries, @Suspended final AsyncResponse asyncResponse){
			List<Entry> auxEntries = new ArrayList<Entry>();
			boolean[] searchables = conf.getOpIndex("%");
			System.out.println(searchables);
			System.out.println(entries.size());
			for(int i = 0 ; i < entries.size() ; i++){
				Entry n = entries.get(i);
				Entry specialEntry = new Entry();
				List<Object> values = n.values;
				for(int j = 0 ; j < values.size() ; j ++){
					if(values.get(j)!=null && searchables[j]){ //se for diferente de null e for um campo de % (search)
						System.out.println("[SEARCHENTRYOR] Searchable:" + i );
						if(activeEncryption)
							specialEntry.addCustomElem(conf.encryptElem(j, values.get(j)));
						else
							specialEntry.addCustomElem(values.get(j));
					}else{
						specialEntry.addCustomElem(null);
					}
				}
				auxEntries.add(specialEntry);
			}
			
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchEntryOr sent = new SearchEntryOr(System.nanoTime(),auxEntries,activeEncryption);
			Future<Object> future = Patterns.ask(proxy, sent, timeout);
			future.onComplete(new OnComplete<Object>() {

	            public void onComplete(Throwable failure, Object result) throws Exception {
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		EntrySet res = (EntrySet) result;
	            		List<Entry> seqlist = res.set();
	            		List<Entry> decryptedEntries = new ArrayList<Entry>();
	            		for(Entry n : seqlist)
	            			decryptedEntries.add(conf.decryptEntry(n));
	                	if(res.set()!= null){
	                		asyncResponse.resume(Response.ok().entity(decryptedEntries).build());
	                	}
	                	else
	                		asyncResponse.resume(Response.ok().entity(false).build());
	            	}
	            }
	        }, actorSystem.dispatcher());
		
		}
		
		@POST
		@Path("/searchentryand")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response searchEntryAND(@QueryParam("jsone")String jsonOne,@QueryParam("jstwo")String jsonTwo, @QueryParam("jsthree")String jsonThree, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}
		
		@GET
		@Path("/orderls")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response orderLS(@HeaderParam("pos") int pos, @Suspended final AsyncResponse asyncResponse){	
			return Response.ok().build();
		}
		
		@GET
		@Path("/ordersl")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response orderSL(@HeaderParam("pos") int pos, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}
		
		@POST
		@Path("/searchgt")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response searchGt(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}
		
		@POST
		@Path("/searchgteq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response searchGtEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){	
			return Response.ok().build();
		}
		
		@POST
		@Path("/searchlt")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response searchLt(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){		
			return Response.ok().build();
		}
		
		@POST
		@Path("/searchlteq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response searchLtEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}

}
