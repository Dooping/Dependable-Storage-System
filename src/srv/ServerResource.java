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
	@Context boolean encrypt;
	String value = "default";
	Entry dummyEntry = new Entry(1,"two",3,"four",5,"six");
	EntryConfig conf;
	
	public ServerResource(){
		conf = new EntryConfig(EntryConfig.CONF_FILE);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ManagedAsync
	public void get(@Suspended final AsyncResponse asyncResponse){
		asyncResponse.resume(Response.ok().entity(conf.getConfigString()+"#Encrypted:"+encrypt).build());
	}
	
	@POST
	@Path("/putset")
	@Consumes(MediaType.APPLICATION_JSON)
	@ManagedAsync
	public void putset(@HeaderParam("key") String key, Entry entry, @Suspended final AsyncResponse asyncResponse){
		Entry n = entry;
		
		if(encrypt)
			n = conf.encryptEntry(entry);
		else{
			//convert Integers to bigintegers, the order remain as long
			Entry specialEntry = new Entry();
			Object[] types = conf.getTypes();
			boolean[] opsLess = conf.getOpIndex("<");
			boolean[] opsLesseq = conf.getOpIndex("<=");
			boolean[] opsGreat = conf.getOpIndex(">");
			boolean[] opsGreateq = conf.getOpIndex(">=");
			List<Object> vals = n.values;
			int size = vals.size();
			for(int i = 0 ; i < size ; i++){
				if(types[i] instanceof Integer){
					if(opsLess[i] || opsLesseq[i] || opsGreat[i] || opsGreateq[i]){
						specialEntry.addCustomElem(new Long((Integer)vals.get(i)) );
					}else{
						int j = (int)vals.get(i);
						specialEntry.addCustomElem(new BigInteger(Integer.toString(j)));
					}
				}else{
					specialEntry.addCustomElem((String)vals.get(i));
				}
			}
			n = specialEntry;
		}
		
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		APIWrite write = new APIWrite(System.nanoTime(), key,"clientidip",n);
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
            		if(encrypt){
	            		ReadResult res = (ReadResult)result;
	            		asyncResponse.resume(Response.ok().entity(conf.decryptEntry(res.v())).build());
            		}else{
            			//deliver as it is (uncrypted)
            			ReadResult res = (ReadResult)result;
	            		asyncResponse.resume(Response.ok().entity(res.v()).build());
	            		
	            	}
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
	            		if(encrypt)
	            			entry.values.add(pos,conf.encryptElem(pos, obj));
	            		else{
	            			Object[] types = conf.getTypes();
	            			if(types[pos] instanceof Integer)
	            				entry.values.add(pos,new BigInteger(Integer.toString((int)obj)));
	            			else
	            				entry.values.add(pos,obj);
	            		}
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
            		if(res.v()!=null){
            			if(encrypt)
            				asyncResponse.resume(Response.ok().entity(conf.decryptElem(pos, res.v().getElem(pos))).build());
            			else{ 
            				//deliver as it is (uncrypteds)
            				asyncResponse.resume(Response.ok().entity(res.v().getElem(pos)).build());
            			}
            		}else
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
		                        		if(encrypt){
		                        		try{
			                        		ReadResult res2 = (ReadResult)result;
			        	            		Entry entry2 = res2.v();
			        	            		BigInteger big1Code = (BigInteger) entry1.getElem(pos);
			        	            		BigInteger big2Code = (BigInteger) entry2.getElem(pos); 
			        	            		PaillierKey pk = (PaillierKey)conf.keys.get(pos).getKey(0);
			        	            		BigInteger res = HomoAdd.sum(big1Code, big2Code, pk.getNsquare());
			                        		asyncResponse.resume(Response.ok().entity(HomoAdd.decrypt(res, pk).toString()).build());
			                        		}catch(Exception e){
			                        			e.printStackTrace();
			                        		}
		                        		}else{
		                        			//deliver as it is (uncrypted)
		                        			ReadResult res2 = (ReadResult)result;
			        	            		Entry entry2 = res2.v();
			        	            		BigInteger firstVal = (BigInteger)entry1.getElem(pos);
			        	            		BigInteger secondVal = (BigInteger)entry2.getElem(pos);
			        	            		asyncResponse.resume(Response.ok().entity(firstVal.add(secondVal)).build());
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
			SumAll sum;
			if(encrypt){
				PaillierKey pkey = (PaillierKey) conf.keys.get(pos).getKey(0);
				sum = new SumAll(System.nanoTime(),pos,encrypt,pkey.getNsquare());
			}else{
				sum = new SumAll(System.nanoTime(),pos,encrypt,null);
			}
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
	                		if(encrypt){
		                		BigInteger big = res.res();
		                		BigInteger truePaiVal = HomoAdd.decrypt(big, (PaillierKey)conf.keys.get(pos).getKey(0));
		                		asyncResponse.resume(Response.ok().entity(truePaiVal.toString()).build());
	                		}else{
	                			//deliver as it is (uncrypted)
	                			System.out.println("HERE:"+result.toString());
	                			asyncResponse.resume(Response.ok().entity(res.res().toString()).build());
	                		}
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
			//missing: check if encryption is active > what to send to Read?
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
		                        		if(encrypt){
			                        		try{
				                        		ReadResult res2 = (ReadResult)result;
				        	            		Entry entry2 = res2.v();
				        	            		BigInteger big1Code = (BigInteger) entry1.getElem(pos);
				        	            		BigInteger big2Code = (BigInteger) entry2.getElem(pos); 
				        	            		RSAPublicKey pubkey = (RSAPublicKey)conf.keys.get(pos).getKey(0);
				        	            		RSAPrivateKey prikey = (RSAPrivateKey)conf.keys.get(pos).getKey(1);
				        	            		BigInteger product = HomoMult.multiply(big1Code, big2Code, pubkey);
				                        		asyncResponse.resume(Response.ok().entity(HomoMult.decrypt(prikey,product).toString()).build());
			                        		}catch(Exception e){
			                        			e.printStackTrace();
			                        		}
		                        		}else{
		                        			//deliver as it is (uncrypted)
		                        			try{
		                        				ReadResult res2 = (ReadResult)result;
				        	            		Entry entry2 = res2.v();
				        	            		BigInteger firstVal = (BigInteger)entry1.getElem(pos);
				        	            		BigInteger secondVal = (BigInteger)entry2.getElem(pos);
				        	            		asyncResponse.resume(Response.ok().entity(firstVal.multiply(secondVal)).build());
			                        		}catch(Exception e){
			                        			e.printStackTrace();
			                        		}
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
			MultAll mult ;
			//missing: check if encryption is active > what to send to MultAll?
			if(encrypt){
				RSAPublicKey rsapubkey = (RSAPublicKey)conf.keys.get(pos).getKey(0);
				mult = new MultAll(System.nanoTime(),pos,encrypt,rsapubkey);
			}else{
				mult = new MultAll(System.nanoTime(),pos,encrypt,null);
			}
			Future<Object> future = Patterns.ask(proxy, mult, timeout);
			future.onComplete(new OnComplete<Object>() {

	            public void onComplete(Throwable failure, Object result) throws Exception {
	            	
	            	if(failure != null){
	            		if(failure.getMessage() != null)
	            			asyncResponse.resume(Response.serverError().entity(failure.getMessage()).build());
	            		else
	            			asyncResponse.resume(Response.serverError());
	            	}else{
	            		if(encrypt){
		            		SumMultAllResult res = (SumMultAllResult) result;
		            		BigInteger big = res.res();
		                	RSAPrivateKey rsaprivKey =(RSAPrivateKey) conf.keys.get(pos).getKey(1);
		                	BigInteger trueRSAVal = HomoMult.decrypt(rsaprivKey, big);
		                	System.out.println("[MULTALL]:"+trueRSAVal.toString());
		                	asyncResponse.resume(Response.ok().entity(trueRSAVal.toString()).build());
	            		}else{
	            			//deliver as it is ( uncrypted )
	            			SumMultAllResult res = (SumMultAllResult) result;
		            		BigInteger big = res.res();
		                	asyncResponse.resume(Response.ok().entity(big.toString()).build());
	            		}
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searcheq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String val =(String) jdata.get(0);
			if(encrypt)
				val = (String)conf.encryptElem(pos, val);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchEq seq = new SearchEq(System.nanoTime(), pos, val);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
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
			if(encrypt)
				val = (String)conf.encryptElem(pos, val);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchNEq seq = new SearchNEq(System.nanoTime(), pos, val);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	
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
			boolean[] searchables = conf.getOpIndex("%");
			
			List<Object> values = entry.values;
			for(int i = 0 ; i < values.size() ; i ++){
				if(values.get(i)!=null && searchables[i]){ //se for diferente de null e for um campo de % (search)
					if(encrypt)
						specialEntry.addCustomElem(conf.encryptElem(i, values.get(i)));
					else
						specialEntry.addCustomElem(values.get(i));
				}else{
					specialEntry.addCustomElem(null);
				}
			}
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(5, "seconds"));
			SearchEntry sent = new SearchEntry(System.nanoTime(),specialEntry,encrypt);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
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
						if(encrypt)
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
			SearchEntryOr sent = new SearchEntryOr(System.nanoTime(),auxEntries,encrypt);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searchentryand")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchEntryAND(List<Entry> entries, @Suspended final AsyncResponse asyncResponse){
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
						if(encrypt)
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
			SearchEntryAnd sent = new SearchEntryAnd(System.nanoTime(),auxEntries,encrypt);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@GET
		@Path("/orderls")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void orderLS(@HeaderParam("pos") int pos, @Suspended final AsyncResponse asyncResponse){	
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			OrderLS orderls = new OrderLS(System.nanoTime(),pos);
			Future<Object> future = Patterns.ask(proxy, orderls, timeout);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@GET
		@Path("/ordersl")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void orderSL(@HeaderParam("pos") int pos, @Suspended final AsyncResponse asyncResponse){
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			OrderSL orderls = new OrderSL(System.nanoTime(),pos);
			Future<Object> future = Patterns.ask(proxy, orderls, timeout);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searcheqint")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchEqInt(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String res =(String) jdata.get(0);
			int val =Integer.parseInt(res);
			long secVal = new Long(Integer.parseInt(res));
			if(encrypt)
				secVal = (Long)conf.encryptElem(pos, val);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchEqInt seq = new SearchEqInt(System.nanoTime(), pos, secVal);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searchgt")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchGt(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String res =(String) jdata.get(0);
			int val =Integer.parseInt(res);
			long secVal = new Long(Integer.parseInt(res));
			if(encrypt)
				secVal = (Long)conf.encryptElem(pos, val);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchGt seq = new SearchGt(System.nanoTime(), pos, secVal);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searchgteq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchGtEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){	
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String res =(String) jdata.get(0);
			int val =Integer.parseInt(res);
			long secVal = new Long(Integer.parseInt(res));
			if(encrypt)
				secVal = (Long)conf.encryptElem(pos, val);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchGtEq seq = new SearchGtEq(System.nanoTime(), pos, secVal);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searchlt")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchLt(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){		
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String res =(String) jdata.get(0);
			int val =Integer.parseInt(res);
			long secVal = new Long(Integer.parseInt(res));
			if(encrypt)
				secVal = (Long)conf.encryptElem(pos, val);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchLt seq = new SearchLt(System.nanoTime(), pos, secVal);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}
		
		@POST
		@Path("/searchlteq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public void searchLtEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			JSONObject o = new JSONObject(json);
			JSONArray jdata = o.getJSONArray("element");
			String res =(String) jdata.get(0);
			int val =Integer.parseInt(res);
			long secVal = new Long(Integer.parseInt(res));
			if(encrypt)
				secVal = (Long)conf.encryptElem(pos, val);
			ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
			Timeout timeout = new Timeout(Duration.create(2, "seconds"));
			SearchLtEq seq = new SearchLtEq(System.nanoTime(), pos, secVal);
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
		            	if(seqlist!= null){
		            		if(encrypt){
		            			List<Entry> decryptedEntries = new ArrayList<Entry>();
					            for(Entry n : seqlist){
					            	decryptedEntries.add(conf.decryptEntry(n));
					            }
				                asyncResponse.resume(Response.ok().entity(decryptedEntries).build());	
		            		}else{
		            			//deliver as it is (uncrypted)
		            			asyncResponse.resume(Response.ok().entity(seqlist).build());
		            		}
			            }else{
			               asyncResponse.resume(Response.ok().entity(false).build());
			            }
	            	}
	            }
	        }, actorSystem.dispatcher());
		}

}
