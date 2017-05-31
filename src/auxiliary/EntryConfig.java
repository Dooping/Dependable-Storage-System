package auxiliary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.crypto.SecretKey;

import Datatypes.Entry;
import hlib.hj.mlib.HomoAdd;
import hlib.hj.mlib.HomoDet;
import hlib.hj.mlib.HomoMult;
import hlib.hj.mlib.HomoOpeInt;
import hlib.hj.mlib.HomoRand;
import hlib.hj.mlib.HomoSearch;
import hlib.hj.mlib.PaillierKey;

public class EntryConfig {

	public static final String CONF_FILE = "conf.txt";
	public static final String DETERMINISTIC_EQUAL = "=";
	public static final String DETERMINISTIC_EQUIV = "<>";
	public static final String ORDER_LESS = "<";
	public static final String ORDER_GREAT = ">";
	public static final String ORDER_LESS_EQUAL = "<=";
	public static final String ORDER_GREAT_EQUAL = ">=";
	public static final String SEARCHABLE = "%";
	public static final String PAILIER = "+";
	public static final String RSA = "&";
	public static final String RAND = ".";
		
	private File file;
	private FileReader fr;
	private BufferedReader br;
	private String filename;
	private Object[] types;
	private String[] ops;
	public HashMap<String, KeyStorage> keys;
	public String configString;
	
	public EntryConfig(String filename){
		this.filename = filename;
		file = new File(filename);
		configString = "";
		if(file.exists())
			load();
		keys = new HashMap<String, KeyStorage>(); //stores the keys in order to decrypt
		//initKeyStorage();
	}

	public String getConfigString(){
		return configString;
	}
	
	/*private void initKeyStorage(){
		for(int i = 0 ; i < types.length ; i ++)
			keys.add(new KeyStorage());
	}*/
	
	public Entry encryptEntry(Entry raw, String client){
		Entry newEntry = new Entry();
		for(int i = 0 ; i < raw.size() ; i++)
			newEntry.addCustomElem(this.encryptElem(i, raw.getElem(i), client));
		return newEntry;
}
	
	public Entry decryptEntry(Entry raw, String client){
		Entry newEntry = new Entry();
		for(int i = 0 ; i < raw.size() ; i++)
			newEntry.addCustomElem(this.decryptElem(i, raw.getElem(i), client));
		return newEntry;
	}
	
	public Object decryptElem(int pos, Object elem, String client){
		if(!keys.containsKey(client))
			this.createKeys(client);
		
		KeyStorage clientKey = keys.get(client);
		
			switch(ops[pos]){
			case EntryConfig.DETERMINISTIC_EQUIV:
			case EntryConfig.DETERMINISTIC_EQUAL:
				SecretKey dkey = (SecretKey)clientKey.getKey(pos); //gets the KeyStorage for this operand and then get the Key
				String cryptDetVal = (String)elem;
				return HomoDet.decrypt(dkey, cryptDetVal);
			case EntryConfig.ORDER_GREAT:
			case EntryConfig.ORDER_LESS_EQUAL:
			case EntryConfig.ORDER_GREAT_EQUAL:
			case EntryConfig.ORDER_LESS:
				long okey = (long)clientKey.getKey(pos);
				HomoOpeInt ope = new HomoOpeInt(okey);
				long cryptOrdVal = (long) elem;
				return ope.decrypt(cryptOrdVal);
			case EntryConfig.SEARCHABLE:
				SecretKey skey = (SecretKey)clientKey.getKey(pos); 
				String cryptSerVal = (String)elem;
				return HomoSearch.decrypt(skey, cryptSerVal);
			case EntryConfig.PAILIER:
				try{
					PaillierKey pkey = (PaillierKey)clientKey.getKey(pos); 
					BigInteger cryptPaiVal = (BigInteger)elem;
					return HomoAdd.decrypt(cryptPaiVal, pkey);
				}catch(Exception e){
					e.printStackTrace();
				}
				break;
			case EntryConfig.RSA:
				RSAPrivateKey privKey = (RSAPrivateKey)((KeyPair)clientKey.getKey(pos)).getPrivate();
				BigInteger cryptRSAVal = (BigInteger) elem;
				return HomoMult.decrypt(privKey, cryptRSAVal);
			case EntryConfig.RAND:
				Object[] arr = (Object[])clientKey.getKey(pos);
				SecretKey randKey = (SecretKey) arr[0];
				byte[] iv = (byte[])arr[1];
				return HomoRand.decrypt(randKey, iv, (String)elem);
			}
		return elem;
	}
	
	public Object encryptElem(int pos, Object elem, String client){
		if(!keys.containsKey(client))
			this.createKeys(client);
		
		KeyStorage clientKey = keys.get(client);
		
		int auxInt = 0;
		String auxString = "";
		
		if(types[pos] instanceof Integer)
			auxInt = (int)elem;
		else
			auxString = (String)elem;
		
		switch(ops[pos]){
		case EntryConfig.DETERMINISTIC_EQUIV:
		case EntryConfig.DETERMINISTIC_EQUAL:
			SecretKey dkey = (SecretKey)clientKey.getKey(pos);
			return HomoDet.encrypt(dkey, auxString);
		case EntryConfig.ORDER_GREAT:
		case EntryConfig.ORDER_LESS_EQUAL:
		case EntryConfig.ORDER_GREAT_EQUAL:
		case EntryConfig.ORDER_LESS:
			long okey = (long)clientKey.getKey(pos);
			HomoOpeInt ope = new HomoOpeInt(okey);
			return ope.encrypt(auxInt);
		case EntryConfig.SEARCHABLE:
			SecretKey skey = (SecretKey)clientKey.getKey(pos); 
			return HomoSearch.encrypt(skey, auxString);
		case EntryConfig.PAILIER:
			try{
				BigInteger big;
				PaillierKey pkey = (PaillierKey)clientKey.getKey(pos); 
				big = new BigInteger(Integer.toString(auxInt));
				return HomoAdd.encrypt(big, pkey);
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		case EntryConfig.RSA:
			BigInteger bigOne;
			RSAPublicKey rsaPublicKey = (RSAPublicKey)((KeyPair)clientKey.getKey(pos)).getPublic();
			bigOne = new BigInteger(Integer.toString(auxInt));
			return HomoMult.encrypt(rsaPublicKey,bigOne);
		case EntryConfig.RAND:
			try{
				Object[] arr = (Object[])clientKey.getKey(pos);
				SecretKey randKey = (SecretKey) arr[0];
				byte[] iv = (byte[])arr[1];
				return HomoRand.encrypt(randKey, iv, auxString);
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		}
		return elem;
		
	
	}
	
	public void load(){

		try{
			fr = new FileReader(filename);
			br = new BufferedReader(fr);
			
			//Read the Entry type structure
			String line1 = br.readLine();
			String[] typesAux = line1.split(" ");
			types = new Object[typesAux.length];
			for(int i = 0 ; i < typesAux.length ; i++){
				if(typesAux[i].equalsIgnoreCase("int"))
					types[i] = new Integer(0);
				else
					types[i] = new String();
			}
			
			//Read the ops allowed on each column/values
			String line2 = br.readLine();
			ops = line2.split(" ");
			
			configString = configString + line1 + "#" + line2;
		
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public Object[] getTypes(){
		return types;
	}
	
	public String[] getOps(){
		return ops;
	}
	
	public boolean[] getOpIndex(String code){
		boolean[] indexs = new boolean[ops.length];
		for(int i = 0 ; i < ops.length ; i++){
			if(ops[i].equalsIgnoreCase(code))
				indexs[i] = true;
		}
		return indexs;
	}
	
	private void createKeys(String client){
		KeyStorage clientKeys = new KeyStorage();
		for(int i = 0; i < ops.length; i++)
			switch(ops[i]){
			case EntryConfig.DETERMINISTIC_EQUIV:
			case EntryConfig.DETERMINISTIC_EQUAL:
				SecretKey dkey = HomoDet.generateKey();
				clientKeys.addKey(dkey);
				break;
			case EntryConfig.ORDER_GREAT:
			case EntryConfig.ORDER_LESS_EQUAL:
			case EntryConfig.ORDER_GREAT_EQUAL:
			case EntryConfig.ORDER_LESS:
				long key = HomoOpeInt.generateKey();
				clientKeys.addKey(key);
				break;
			case EntryConfig.SEARCHABLE:
				SecretKey skey = HomoSearch.generateKey();
				clientKeys.addKey(skey);
				break;
			case EntryConfig.PAILIER:
				PaillierKey pk = HomoAdd.generateKey();
				clientKeys.addKey(pk);
				break;
			case EntryConfig.RSA:
				KeyPair keyPair = HomoMult.generateKey();
				clientKeys.addKey(keyPair);
				break;
			case EntryConfig.RAND:
				SecretKey randKey = HomoRand.generateKey();
				byte[] iv = HomoRand.generateIV();
				clientKeys.addKey(new Object[]{randKey, iv});
				break;
		}
		keys.put(client, clientKeys);
	}
}
