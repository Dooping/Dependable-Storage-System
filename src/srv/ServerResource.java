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
	public Response putset(@HeaderParam("key") String key, Entry entry){
		System.out.println("[PUTSET] " + entry.toString());
		System.out.println(entry.toString());
		return Response.ok(new JSONObject().put("result", key).toString(), MediaType.APPLICATION_JSON).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getset")
	public Response getSet(@HeaderParam("key") String key){		
		System.out.println("[GETSET]");
		return Response.ok(dummyEntry, MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Path("/addelem")
	public Response addElem(@HeaderParam("key") String key){
		System.out.println("[ADDELEM]");
		//Add random element to end of list?
		return Response.ok().build();
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
	public Response writeElem(@HeaderParam("key") String key, String json,@HeaderParam("pos") int pos){
		System.out.println("writeElem called");
		JSONObject o = new JSONObject(json);
		Object obj = o.get("element");
		System.out.println(obj);
		return Response.ok().build();
	}
	
	@GET
	@Path("/readelem")
	@Produces(MediaType.APPLICATION_JSON)
	public Response readElem(@HeaderParam("key") String key,@HeaderParam("pos") int pos){
		System.out.println("[READELEM] Position:"+ pos + " Value:"+dummyEntry.getElem(pos));
		return Response.ok(dummyEntry.getElem(pos), MediaType.APPLICATION_JSON).build();
	}
	
	@GET
	@Path("/iselem")
	@Produces(MediaType.APPLICATION_JSON)
	public Response isElem(@HeaderParam("key")String key,@QueryParam("elem") String elem){
		//In order to check if this element exists in list of values, how do we
		//know if we should compare it with string or int when we have a list of Objects?
		System.out.println("[ISELEM] Element:"+ elem + " Value: (need to check if elem exists)");
		return Response.ok().build();
	}

}
