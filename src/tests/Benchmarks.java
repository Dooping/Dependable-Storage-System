package tests;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import Datatypes.Entry;

public class Benchmarks {
	static final String[] STRINGS = new String[]{"one","two","three","four","five","six", "seven", "eight"};

	WebTarget target;
	File resFile;
	FileWriter fw;
	StringBuilder sb;
	boolean activeEncryption;
	Object[] config;
	
	public Benchmarks(WebTarget target,boolean activeEncryption, Object[] config) throws Exception{
		
		this.target = target;
		if(activeEncryption)
			resFile = new File("resultsEncrypted.csv");
		else
			resFile = new File("resultsUncrypted.csv");
		fw = new FileWriter(resFile);
		sb = new StringBuilder();

		sb.append("benchmark,operation,status,time,encrypted\n");
		
		fw.write(sb.toString());
		fw.close();
		
		this.activeEncryption = activeEncryption;
		this.config = config;
	
	}
	
	public void appendStringBuilder(String benchmark, String op, int status,String time){
		String entry = benchmark + ',' + op + ',' + status + ',' + time+','+activeEncryption+'\n';
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
					.request().header("key", "mykey"+i).async().
					post(Entity.entity(Entry.randomEntry(config, STRINGS, 10), MediaType.APPLICATION_JSON));
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = String.format("%.7f",ms).replace(',','.');
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
					.header("key", "mykey"+i)
					.async()
					.get();
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("2","GET",status,msstring);
		}
		
		fw.write(sb.toString());
		fw.close();
		
	}
	
	//50 PUTSET & 50 GESET ALTER
	public void benchmark3() throws Exception{
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd ;
		int status;
		
		//50 PUTSET & 50 GESET ALTER
		for(int i = 0 ; i < 50 ; i ++){
			
			//PUTSET
			nanotimeStart = System.nanoTime();
			value = target.path("/server/putset")
					.request().header("key", "mykey"+i).async().
					post(Entity.entity(Entry.randomEntry(config, STRINGS, 10), MediaType.APPLICATION_JSON));
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("3","PUT",status,msstring);
	        
	        //GETSET
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/getset")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.header("key", "mykey"+i)
					.async()
					.get();
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("3","GET",status,msstring);
		}
		
		fw.write(sb.toString());
		fw.close();
		
	}
	
	//50 ADDELEM & 50 READELEM ALTER
	public void benchmark4() throws Exception{
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd ;
		int status;
		
		//50 ADDELEM & 50 READELEM ALTER
		for(int i = 0 ; i < 50 ; i ++){
			
			//ADDELEM
			nanotimeStart = System.nanoTime();
			value = target.path("/server/addelem")
					.request().header("key", "mykey"+i).async().post(null);
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("4","ADD",status,msstring);
	        
	        //READELEM
	        nanotimeStart = System.nanoTime();
			value = target.path("/server/readelem")
					.request().header("key", "mykey"+i).header("pos", 1)
					.accept(MediaType.APPLICATION_JSON)
					.async()
					.get();
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("4","READ",status,msstring);
		}
		
		fw.write(sb.toString());
		fw.close();
		
		
	}
	
	public void benchmark5() throws Exception{
		
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd ;
		int status;
		
		//10 PUTSETS 10 GETSETS 10 ADDELEMENTS 10 WRITEELEMS 10 READELEMS
		for(int i = 0 ; i < 30 ; i ++){
			
			//PUTSET
			nanotimeStart = System.nanoTime();
			value = target.path("/server/putset")
					.request().header("key", "mykey"+i).async().
					post(Entity.entity(Entry.randomEntry(config, STRINGS, 10), MediaType.APPLICATION_JSON));			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("5","PUT",status,msstring);
			
	        //GETSET
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/getset")
					.request()
					.accept(MediaType.APPLICATION_JSON)
					.header("key", "mykey"+i)
					.async()
					.get();		
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("5","GET",status,msstring);
	         
	        //WRITELEM
	        nanotimeStart = System.nanoTime();
	        JSONObject jsonobj = new JSONObject();
			jsonobj.append("element", 2);
			value = target.path("/server/writeelem").request()
					.header("key", "mykey"+i).header("pos", 2).async().post(Entity.entity(jsonobj.toString(), MediaType.APPLICATION_JSON));			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("5","WRITE",status,msstring);
	        
	        //READELEM
	        nanotimeStart = System.nanoTime();
			value = target.path("/server/readelem")
					.request().header("key", "mykey"+i).header("pos", 1)
					.accept(MediaType.APPLICATION_JSON)
					.async()
					.get();			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("5","READ",status,msstring);
	             
	        //ADDELEM
			nanotimeStart = System.nanoTime();
			value = target.path("/server/addelem")
					.request().header("key", "mykey"+i).async().post(null);			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("5","ADD",status,msstring);
	        
	        //ISELEM
	        nanotimeStart = System.nanoTime();
	        JSONObject json = new JSONObject();
			json.append("element", "two");
			value = target.path("/server/iselem").request()
					.header("key","mykey"+i).async().post(Entity.entity(json.toString(),MediaType.APPLICATION_JSON));
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("5","ISEL",status,msstring);
	        
	        //REMOVESET
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/removeset").request().header("key", "mykey"+i).async().delete();
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("5","REM",status,msstring);
	        
		}
		
		fw.write(sb.toString());
		fw.close();
		
	}
	
	public void benchmarkE1E3() throws Exception{
		
		target.path("/server/putset")
		.request().header("key", "sbp").async().
		post(Entity.entity(new Entry(1,"two",3,"four",5,"six"), MediaType.APPLICATION_JSON)).get().getStatus();
		
		target.path("/server/putset")
		.request().header("key", "dg").async().
		post(Entity.entity(new Entry(2,"three",4,"five",6,"seven"), MediaType.APPLICATION_JSON)).get().getStatus();
		
		target.path("/server/putset")
		.request().header("key", "csd").async().
		post(Entity.entity(new Entry(20,"sd",3,"asd",5,"csd"), MediaType.APPLICATION_JSON)).get().getStatus();
		
		
		ArrayList<Entry> entries = new ArrayList<Entry>();
		entries.add(new Entry(1,"two",3,"four",5,"six"));
		entries.add(new Entry(2,"three",4,"five",6,"seven"));
		entries.add(new Entry(20,"sd",3,"asd",5,"csd"));
		JSONObject jsonObj = new JSONObject();
		JSONObject jsonObj2 = new JSONObject();
		jsonObj.append("element", "2");
		jsonObj2.append("element", "four");
		
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd ;
		float ms;
		String msstring;
		int status;
		String benchmark = "E1";
		if(activeEncryption)
			benchmark = "E3";
		
		//10 search ops
		for(int i = 0 ; i < 10 ; i ++){
			
			nanotimeStart = System.nanoTime();
			value = target.path("/server/searcheq").request().header("pos", 3).async().post(Entity.entity(jsonObj2.toString(),MediaType.APPLICATION_JSON));
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEQ",status,msstring);
			
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchneq").request().header("pos", 3).async().post(Entity.entity(jsonObj2.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SNEQ",status,msstring);
			
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentry").request().async().post(Entity.entity(new Entry(1,"two",3,"four",5,"six"),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SE",status,msstring);
	       
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentryor").request().async().post(Entity.entity(entries,MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEOR",status,msstring);
	        
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentryand").request().async().post(Entity.entity(entries,MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEAND",status,msstring);
	        
	        nanotimeStart = System.nanoTime();
	        target.path("/server/orderls").request().header("pos", 0).async().get();
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"ORDLS",status,msstring);
	      
	        nanotimeStart = System.nanoTime();
	        target.path("/server/ordersl").request().header("pos", 0).async().get();
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"ORDSL",status,msstring);
	        
	        value = target.path("/server/searcheqint").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEQINT",status,msstring);
	        
	        value = target.path("/server/searchgt").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SGT",status,msstring);
	        
	        value = target.path("/server/searchgteq").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SGTEQ",status,msstring);
	        
	        value = target.path("/server/searchlt").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SLT",status,msstring);
	      
	        value = target.path("/server/searchlteq").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SLTEQ",status,msstring);
	        /*  */
		}
		fw.write(sb.toString());
		fw.close();
	}
	
	public void benchmarkE2E4() throws Exception{
		/*
		target.path("/server/putset")
		.request().header("key", "sbp").async().
		post(Entity.entity(new Entry(1,"two",3,"four",5,"six"), MediaType.APPLICATION_JSON));
		
		target.path("/server/putset")
		.request().header("key", "dg").async().
		post(Entity.entity(new Entry(2,"three",4,"five",6,"seven"), MediaType.APPLICATION_JSON));
		
		target.path("/server/putset")
		.request().header("key", "csd").async().
		post(Entity.entity(new Entry(20,"sd",3,"asd",5,"csd"), MediaType.APPLICATION_JSON));
		*/
		ArrayList<Entry> entries = new ArrayList<Entry>();
		entries.add(new Entry(1,"two",3,"four",5,"six"));
		entries.add(new Entry(2,"three",4,"five",6,"seven"));
		entries.add(new Entry(20,"sd",3,"asd",5,"csd"));
		JSONObject jsonObj = new JSONObject();
		JSONObject jsonObj2 = new JSONObject();
		jsonObj.append("element", "2");
		jsonObj2.append("element", "four");
		
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value;
		long nanotimeStart,nanotimeEnd ;
		float ms;
		String msstring;
		int status;
		String benchmark = "E2";
		if(activeEncryption)
			benchmark = "E4";
		
		//100 search ops
		for(int i = 0 ; i < 100 ; i ++){
			
			nanotimeStart = System.nanoTime();
			value = target.path("/server/sum").request().header("keyOne","sbp").header("keyTwo", "csd").header("pos", 2).async().get();
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SUM",status,msstring);
			
			nanotimeStart = System.nanoTime();
			value = target.path("/server/sumall").request().header("pos", 2).async().get();
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SUMALL",status,msstring);
			
			nanotimeStart = System.nanoTime();
			value = target.path("/server/mult").request().header("keyOne","sbp").header("keyTwo", "csd").header("pos", 4).async().get();
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"MULT",status,msstring);
			
			nanotimeStart = System.nanoTime();
			value = target.path("/server/multall").request().header("pos", 4).async().get();
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"MULTALL",status,msstring);

			nanotimeStart = System.nanoTime();
			value = target.path("/server/searcheq").request().header("pos", 3).async().post(Entity.entity(jsonObj2.toString(),MediaType.APPLICATION_JSON));
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEQ",status,msstring);
			
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchneq").request().header("pos", 3).async().post(Entity.entity(jsonObj2.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SNEQ",status,msstring);
			
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentry").request().async().post(Entity.entity(new Entry(1,"two",3,"four",5,"six"),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SE",status,msstring);
	       
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentryor").request().async().post(Entity.entity(entries,MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEOR",status,msstring);
	        
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentryand").request().async().post(Entity.entity(entries,MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEAND",status,msstring);
	      
	        value = target.path("/server/searcheqint").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEQINT",status,msstring);
	        
	        value = target.path("/server/searchgt").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SGT",status,msstring);
	        
	        value = target.path("/server/searchgteq").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SGTEQ",status,msstring);
	        
	        value = target.path("/server/searchlt").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SLT",status,msstring);
	      
	        value = target.path("/server/searchlteq").request().header("pos", 0).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SLTEQ",status,msstring);
	        /*  */
		}
		fw.write(sb.toString());
		fw.close();
	}
}
