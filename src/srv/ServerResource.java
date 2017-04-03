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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import Datatypes.*;

import org.json.*;

@Path("/server")
public class ServerResource {

	String value = "default";
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String get() {
		return value;
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
		System.out.println("putset called");
		System.out.println(entry.toString());
		return Response.ok(key).build();
	}
	
	@DELETE
	@Path("/removeset")
	public Response removeSet(@HeaderParam("key") String key){
		System.out.println("removeSet called");
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

}
