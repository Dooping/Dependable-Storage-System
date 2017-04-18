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

import messages.*;

import org.json.*;

@Path("/server")
public class ServerResource {

	@Context ActorSystem actorSystem;
	String value = "default";
	Entry dummyEntry = new Entry(1,"two",3,"four",5,"six");

	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    @ManagedAsync
    public void asyncGet(@Suspended final AsyncResponse asyncResponse) {
		asyncResponse.resume(Response.ok(dummyEntry,MediaType.APPLICATION_JSON).build());
    }

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public void set( String val) {
		value = val;
	}
	
	@POST
	@Path("/putset")
	@Consumes(MediaType.APPLICATION_JSON)
	@ManagedAsync
	public void putset(@HeaderParam("key") String key, Entry entry, @Suspended final AsyncResponse asyncResponse){
		System.out.println("[PUTSET] " + entry.toString());
		System.out.println(entry.toString());
		
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		
		System.out.println(proxy.pathString());
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
		System.out.println("[GETSET]");
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
                	System.out.println(result);
            		asyncResponse.resume(Response.ok().entity(res.v()).build());
            	}
            }
        }, actorSystem.dispatcher());
		
	}
	
	@POST
	@Path("/addelem")
	public void addElem(@HeaderParam("key") String key,@Suspended final AsyncResponse asyncResponse){
		System.out.println("[ADDELEM]");
		
		ActorSelection proxy = actorSystem.actorSelection("/user/proxy");
		Timeout timeout = new Timeout(Duration.create(2, "seconds"));
		
		APIWrite write = new APIWrite(System.nanoTime(), key,"clientidip",new Entry(1,"two",3,"four",5,"six"));
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
	
	@DELETE
	@Path("/removeset")
	public Response removeSet(@HeaderParam("key") String key){
		System.out.println("[REMOVESET]");
		return Response.ok().build();
	}
	
	@POST
	@Path("/writeelem")
	@Consumes(MediaType.APPLICATION_JSON)
	public void writeElem(@HeaderParam("key") String key, String json,@HeaderParam("pos") int pos,@Suspended final AsyncResponse asyncResponse){
		System.out.println("[WRITEELEM]");
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
        }, actorSystem.dispatcher());
		
	}
	
	@GET
	@Path("/readelem")
	@Produces(MediaType.APPLICATION_JSON)
	public void readElem(@HeaderParam("key") String key,@HeaderParam("pos") int pos,@Suspended final AsyncResponse asyncResponse){
		System.out.println("[READELEM]");
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
            		System.out.println("READELEM:"+ result);
            		if(res.v()!=null)
            			asyncResponse.resume(Response.ok().entity(res.v().getElem(pos)).build());
            		else
            			asyncResponse.resume(Response.ok().entity(null).build());
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
		System.out.println("[ISELEM] Element:"+ obj + " Value: (need to check if elem exists)");
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
                	System.out.println(result);
                	if(res.v()!=null)
                		asyncResponse.resume(Response.ok().entity(res.v().values.contains(obj)).build());
                	else
                		asyncResponse.resume(Response.ok().entity(false).build());
            	}
            }
        }, actorSystem.dispatcher());
	}

}
