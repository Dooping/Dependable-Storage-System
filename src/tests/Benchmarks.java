package tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import Datatypes.Entry;

public class Benchmarks {

	WebTarget target;
	File resFile;
	FileWriter fw;
	StringBuilder sb;
	
	public Benchmarks(WebTarget target) throws Exception{
		this.target = target;
		resFile = new File("results.csv");
		fw = new FileWriter(resFile);
		sb = new StringBuilder();

		sb.append("benchmark");
		sb.append(',');
		sb.append("operation");
		sb.append(',');
		sb.append("status");
		sb.append(",");
		sb.append("time");
		sb.append('\n');
		
		fw.write(sb.toString());
		fw.close();
	
	}
	
	public void appendStringBuilder(String benchmark, String op, int status,String time ){
		String entry = benchmark + ',' + op + ',' + status + ',' + time+'\n';
		sb.append(entry);
	}
	
	//100 PUTSET
	public void benchmark1() throws Exception{
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd;
		int status;
		for(int i = 0 ; i < 100 ; i ++){
			nanotimeStart = System.nanoTime();
			value = target.path("/server/putset")
					.request().header("key", "mykey").async().
					post(Entity.entity(new Entry(1,"2",3,"4",5,"6"), MediaType.APPLICATION_JSON));
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = "".format("%.7f",ms).replace(',','.');
	        appendStringBuilder("1","PUT",status,msstring);
		}
		
		fw.write(sb.toString());
		fw.close();
	}
	
	//100 GETSET
	public void benchmark2() throws Exception{
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder(); //clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd ;
		int status;
		for(int i = 0 ; i < 100 ; i ++){
			nanotimeStart = System.nanoTime();
			value = target.path("/server/getset")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.async()
					.get();
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = "".format("%.7f",ms).replace(',','.');
	        appendStringBuilder("2","GET",status,msstring);
		}
		
		fw.write(sb.toString());
		fw.close();
		
	}
	
	//50 PUTSET 50 GETSET
	public void benchmark3() throws Exception{
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd ;
		int status;
		
		//50 PUTSET
		for(int i = 0 ; i < 50 ; i ++){
			nanotimeStart = System.nanoTime();
			value = target.path("/server/putset")
					.request().header("key", "mykey").async().
					post(Entity.entity(new Entry(1,"2",3,"4",5,"6"), MediaType.APPLICATION_JSON));
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = "".format("%.7f",ms).replace(',','.');
	        appendStringBuilder("3","PUT",status,msstring);
		}
		
		//50 GETSET
		for(int i = 0 ; i < 100 ; i ++){
			nanotimeStart = System.nanoTime();
			value = target.path("/server/getset")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.async()
					.get();
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = "".format("%.7f",ms).replace(',','.');
	        appendStringBuilder("3","GET",status,msstring);
		}

		fw.write(sb.toString());
		fw.close();
		
	}
	
	public void benchmark4(){
		
	}
	
	public void benchmark5(){
		
	}
	
}
