package tests;


import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;
import Datatypes.Entry;

public class Benchmarks {
	static final String[] STRINGS = new String[]{"one","two","three","four","five","six", "seven", "eight"};
	int intVals[] = {1,2,3,4,5,6,7,8,9};
	int maxNumberEntries = 3; //max number of entries to generate randomly
	WebTarget target;
	File resFile;
	FileWriter fw;
	StringBuilder sb;
	boolean activeEncryption;
	Object[] types;
	String[] ops;
	
	public Benchmarks(WebTarget target,boolean activeEncryption, Object[] types, String[] ops) throws Exception{
		
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
		this.types = types;
		this.ops = ops;
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
			System.out.print(".");
			nanotimeStart = System.nanoTime();
			value = target.path("/server/putset")
					.request().header("key", "mykey"+i).async().
					post(Entity.entity(Entry.randomEntry(types, STRINGS, 10), MediaType.APPLICATION_JSON));
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        float ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        String msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder("1","PUT",status,msstring);
		}
		System.out.println();
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
			System.out.println(".");
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
		System.out.println();
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
			System.out.println(".");
			//PUTSET
			nanotimeStart = System.nanoTime();
			value = target.path("/server/putset")
					.request().header("key", "mykey"+i).async().
					post(Entity.entity(Entry.randomEntry(types, STRINGS, 10), MediaType.APPLICATION_JSON));
			
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
		System.out.println();
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
			System.out.println(".");
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
		System.out.println();
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
			System.out.println(".");
			//PUTSET
			nanotimeStart = System.nanoTime();
			value = target.path("/server/putset")
					.request().header("key", "mykey"+i).async().
					post(Entity.entity(Entry.randomEntry(types, STRINGS, 10), MediaType.APPLICATION_JSON));			
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
		System.out.println();
		fw.write(sb.toString());
		fw.close();
		
	}
	
	public void benchmarkE1E3() throws Exception{
		
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
			
		//List<Entry> randEntries;
		
		//10 search ops
		for(int i = 0 ; i < 10 ; i ++){
			System.out.print(".");
			
	        int randValue = ThreadLocalRandom.current().nextInt(0, STRINGS.length);
	        JSONObject obj1 = new JSONObject();
	        obj1.append("element", STRINGS[randValue]);
	        
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentry").request().async().post(Entity.entity(obj1.toString(),MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SE",status,msstring);
	       
	        List<String> vals = new ArrayList<String>();
	        int randNrVals = ThreadLocalRandom.current().nextInt(0, 10);
	        for(int j = 0 ; j < randNrVals ; j++){
	        	int randIndex = ThreadLocalRandom.current().nextInt(0, STRINGS.length);
	        	vals.add(STRINGS[randIndex]);
	        }
	        
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentryor").request().async().post(Entity.entity(vals,MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEOR",status,msstring);
	       
	        nanotimeStart = System.nanoTime();
	        value = target.path("/server/searchentryand").request().async().post(Entity.entity(vals,MediaType.APPLICATION_JSON));
	        status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,"SEAND",status,msstring);
	        /* */
		}
		System.out.println();
		fw.write(sb.toString());
		fw.close();
	}
	
	public void benchmarkE2E4() throws Exception{
	
		fw = new FileWriter(resFile,true); //the true is to append to the end of file
		sb = new StringBuilder();//clears the previous stringbuilder
		Future<Response> value = null;
		long nanotimeStart,nanotimeEnd ;
		float ms;
		String msstring, op = null;
		int status, randPosSums, randPosMults, randPosSEQ, randVal, randValue, randArray, ranSEQINT, randVal2;
		String benchmark = "E2";
		if(activeEncryption)
			benchmark = "E4";
		
		JSONObject jsonObj = new JSONObject();
		JSONObject jsonObj2 = new JSONObject();
		boolean[] sums = getOpIndex("+");
		boolean[] mults = getOpIndex("&");
		boolean[] searchs = getOpIndex("=");
		ArrayList<boolean[]> orderBooleans = new ArrayList<boolean[]>();
		
		boolean[] opl = getOpIndex("<");
		boolean[] ople = getOpIndex("<=");
		boolean[] opg = getOpIndex(">");
		boolean[] opge = getOpIndex(">=");
		if(opExists(opl))
			orderBooleans.add(opl);
		if(opExists(ople))
			orderBooleans.add(ople);
		if(opExists(opg))
			orderBooleans.add(opg);
		if(opExists(opge))
			orderBooleans.add(opge);

		Random r = new Random(System.currentTimeMillis());
		//100 search ops
		for(int i = 0 ; i < 100 ; i ++){ //99 , pois quando chegar ao 98 , a keyTwo considera 98 +1 (99) que é a ultima
			System.out.print(".");
			int rand = r.nextInt(14);
			
			nanotimeStart = System.nanoTime();
			
			switch(rand){
			case 0:
				randPosSums = randomPosition(sums);
				value = target.path("/server/sum").request().header("keyOne","mykey"+i).header("keyTwo", "mykey"+((i+1)%100)).header("pos", randPosSums).async().get();
				op = "SUM";
				break;
			case 1:
				randPosSums = randomPosition(sums);
				value = target.path("/server/sumall").request().header("pos", randPosSums).async().get();
				op = "SUMALL";
				break;
			case 2:
				randPosMults = randomPosition(mults);
				value = target.path("/server/mult").request().header("keyOne","mykey"+i).header("keyTwo", "mykey"+((i+1)%100)).header("pos", randPosMults).async().get();
				op = "MULT";
				break;
			case 3:
				randPosMults = randomPosition(mults);
				value = target.path("/server/multall").request().header("pos", randPosMults).async().get();
				op = "MULTALL";
				break;
			case 4:
				randPosSEQ = randomPosition(searchs);
				randVal = ThreadLocalRandom.current().nextInt(0, STRINGS.length);
				jsonObj2 = new JSONObject();
		        jsonObj2.append("element", STRINGS[randVal]);
		        value = target.path("/server/searcheq").request().header("pos", randPosSEQ).async().post(Entity.entity(jsonObj2.toString(),MediaType.APPLICATION_JSON));
		        op = "SEQ";
				break;
			case 5:
				randPosSEQ = randomPosition(searchs);
				randVal = ThreadLocalRandom.current().nextInt(0, STRINGS.length);
				jsonObj2 = new JSONObject();
		        jsonObj2.append("element", STRINGS[randVal]);
		        value = target.path("/server/searchneq").request().header("pos", randPosSEQ).async().post(Entity.entity(jsonObj2.toString(),MediaType.APPLICATION_JSON));
		        op = "SNEQ";
				break;
			case 6:
				randValue = ThreadLocalRandom.current().nextInt(0, STRINGS.length);
		        JSONObject obj1 = new JSONObject();
		        obj1.append("element", STRINGS[randValue]);
		        value = target.path("/server/searchentry").request().async().post(Entity.entity(obj1.toString(),MediaType.APPLICATION_JSON));
		        op = "SE";
				break;
			case 7:
				List<String> vals = new ArrayList<String>();
		        int randNrVals = ThreadLocalRandom.current().nextInt(0, 10);
		        for(int j = 0 ; j < randNrVals ; j++){
		        	int randIndex = ThreadLocalRandom.current().nextInt(0, STRINGS.length);
		        	vals.add(STRINGS[randIndex]);
		        }
		        value = target.path("/server/searchentryor").request().async().post(Entity.entity(vals,MediaType.APPLICATION_JSON));
		        op = "SEOR";
				break;
			case 8:
				List<String> vals1 = new ArrayList<String>();
		        int randNrVals1 = ThreadLocalRandom.current().nextInt(0, 10);
		        for(int j = 0 ; j < randNrVals1 ; j++){
		        	int randIndex = ThreadLocalRandom.current().nextInt(0, STRINGS.length);
		        	vals1.add(STRINGS[randIndex]);
		        }
		        value = target.path("/server/searchentryand").request().async().post(Entity.entity(vals1,MediaType.APPLICATION_JSON));
		        op = "SEAND";
				break;
			case 9:
				randArray= ThreadLocalRandom.current().nextInt(0, orderBooleans.size());
		        ranSEQINT = randomPosition(orderBooleans.get(randArray));
		        randVal2 =  ThreadLocalRandom.current().nextInt(0, intVals.length);
		        jsonObj = new JSONObject();
		        jsonObj.append("element", Integer.toString(intVals[randVal2]));
		        value = target.path("/server/searcheqint").request().header("pos", ranSEQINT).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
		        op = "SEQINT";
				break;
			case 10:
				randArray= ThreadLocalRandom.current().nextInt(0, orderBooleans.size());
		        ranSEQINT = randomPosition(orderBooleans.get(randArray));
		        randVal2 =  ThreadLocalRandom.current().nextInt(0, intVals.length);
		        jsonObj = new JSONObject();
		        jsonObj.append("element", Integer.toString(intVals[randVal2]));
		        value = target.path("/server/searchgt").request().header("pos",ranSEQINT).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
		        op = "SGT";
				break;
			case 11:
				randArray= ThreadLocalRandom.current().nextInt(0, orderBooleans.size());
		        ranSEQINT = randomPosition(orderBooleans.get(randArray));
		        randVal2 =  ThreadLocalRandom.current().nextInt(0, intVals.length);
		        jsonObj = new JSONObject();
		        jsonObj.append("element", Integer.toString(intVals[randVal2]));
		        value = target.path("/server/searchgteq").request().header("pos", ranSEQINT).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
		        op = "SGTEQ";
				break;
			case 12:
				randArray= ThreadLocalRandom.current().nextInt(0, orderBooleans.size());
		        ranSEQINT = randomPosition(orderBooleans.get(randArray));
		        randVal2 =  ThreadLocalRandom.current().nextInt(0, intVals.length);
		        jsonObj = new JSONObject();
		        jsonObj.append("element", Integer.toString(intVals[randVal2]));
		        value = target.path("/server/searchlt").request().header("pos", ranSEQINT).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
		        op = "SLT";
				break;
			case 13:
				randArray= ThreadLocalRandom.current().nextInt(0, orderBooleans.size());
		        ranSEQINT = randomPosition(orderBooleans.get(randArray));
		        randVal2 =  ThreadLocalRandom.current().nextInt(0, intVals.length);
		        jsonObj = new JSONObject();
		        jsonObj.append("element", Integer.toString(intVals[randVal2]));
		        value = target.path("/server/searchlteq").request().header("pos", ranSEQINT).async().post(Entity.entity(jsonObj.toString(),MediaType.APPLICATION_JSON));
		        op = "SLTEQ";
				break;
			}
			
			status = value.get().getStatus();
	        nanotimeEnd = System.nanoTime();
	        ms = (nanotimeEnd-nanotimeStart) / 1000000.0f;
	        msstring = String.format("%.7f",ms).replace(',','.');
	        appendStringBuilder(benchmark,op,status,msstring);
			
			
		}
		System.out.println();
		fw.write(sb.toString());
		fw.close();
	}
	
	/*positions: the positions where a certain operation is allowed to execute
	 * gets all those "true" positions, and, returns a random number from those true positions
	 * */
	public int randomPosition(boolean[] positions){
		int size = 0;
		for(boolean b : positions)
			if(b)
				size++;
		int[] auxPositions = new int[size];
		int counter = 0;
		for(int i = 0 ; i < positions.length ; i ++)
			if(positions[i]){
				auxPositions[counter] = i;
				counter++;
			}
		if(size == 0)
			return -1;
		int rand = ThreadLocalRandom.current().nextInt(0, size);
		return auxPositions[rand];
	}
	
	/*
	 *  this is used mostly for the order operations ( < <= > >= ) we need to check
	 *  in what operation should we randomize
	 * */
	public boolean opExists(boolean[] pos){
		for(int i = 0 ; i < ops.length ; i++)
			if(pos[i])
				return true;
		return false;
	}
	
	/*given an operation, returns its position
	 * */
	public boolean[] getOpIndex(String op){
		boolean[] indexs = new boolean[ops.length];
		for(int i = 0 ; i < ops.length ; i++)
			if(ops[i].equalsIgnoreCase(op))
				indexs[i] = true;
		return indexs;
	}
	
}
