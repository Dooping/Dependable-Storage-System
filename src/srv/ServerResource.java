package srv;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import Datatypes.*;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import org.glassfish.jersey.server.ManagedAsync;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;


import org.json.*;

@Path("/server")
public class ServerResource {

	@Context ActorSystem actorSystem;
	String value = "default";
	Entry dummyEntry = new Entry(1,"two",3,"four",5,"six");
	
	@POST
	@Path("/putset")
	@Consumes(MediaType.APPLICATION_JSON)
	@ManagedAsync
	public void putset(@HeaderParam("key") String key, Entry entry, @Suspended final AsyncResponse asyncResponse){
		
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		APIWrite write = new APIWrite(System.nanoTime(), key,"clientidip",dummyEntry);
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
            		asyncResponse.resume(Response.ok().entity(res.v()).build());
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
	            		entry.values.add(pos,obj);
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
            			asyncResponse.resume(Response.ok().entity(res.v().getElem(pos)).build());
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
		public Response sum(@HeaderParam("pos") int pos,@HeaderParam("keyOne")String keyOne,@HeaderParam("keyTwo")String keyTwo, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}

		@GET
		@Path("/sumall")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response sumAll(@HeaderParam("pos")int pos, @Suspended final AsyncResponse asyncResponse){	
			return Response.ok().build();
		}
		
		@GET
		@Path("/mult")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response mult(@HeaderParam("pos") int pos,@HeaderParam("keyOne") String keyOne, @HeaderParam("keyTwo") String keyTwo, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}
		
		@GET
		@Path("/multall")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response multAll(@HeaderParam("pos") int pos, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}
		
		@POST
		@Path("/searcheq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response searchEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}
		
		@POST
		@Path("/searchneq")
		@Produces(MediaType.APPLICATION_JSON)
		@ManagedAsync
		public Response searchNEq(@HeaderParam("pos") int pos, String json, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
		}
		
		@POST
		@Path("/searchentryor")
		@Produces(MediaType.APPLICATION_JSON)
		public Response searchEntryOR(String json, @Suspended final AsyncResponse asyncResponse){
			return Response.ok().build();
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
