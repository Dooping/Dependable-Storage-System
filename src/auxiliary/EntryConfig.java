package auxiliary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
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
	public List<KeyStorage> keys;
	private boolean keysStored;
	
	public EntryConfig(String filename){
		this.filename = filename;
		keysStored = false; // keys are stored only once ( the first entry inserted ) those keys are used for the rest of entries
		file = new File(filename);
		if(file.exists())
			load();
		keys = new ArrayList<KeyStorage>(); //stores the keys in order to decrypt
		initKeyStorage();
		
	}
	
	private void initKeyStorage(){
		for(int i = 0 ; i < types.length ; i ++)
			keys.add(new KeyStorage());
	}
	
	public Entry encryptEntry(Entry raw){

		System.out.println("=====[ENCRYPTING]=====");
		List<Object> vals = raw.values; //the Entry's raw values
		List<Object> crypts = new ArrayList<Object>();  //stores each Entry's encripted values
		int auxInt = 0;
		String auxString = "";
		
		for(int i = 0 ; i < vals.size() ; i++){
			
			if(types[i] instanceof Integer)
				auxInt = (int)vals.get(i);
			else
				auxString = (String) vals.get(i);
			
			switch(ops[i]){
			
				case EntryConfig.DETERMINISTIC_EQUAL:
					SecretKey dkey;
					String encrypt;
					if(keysStored){
						dkey = (SecretKey)keys.get(i).getKey(0);
						encrypt = HomoDet.encrypt(dkey, auxString);
						crypts.add(encrypt);
					}else{
						dkey = HomoDet.generateKey();
						encrypt = HomoDet.encrypt(dkey, auxString);
						crypts.add(encrypt);
						keys.get(i).addKey(dkey);
					}
					
					System.out.println("[DETER_EQUAL]Encrypting: " + auxString);
					System.out.println("[DETER_EQUAL]Encrypted: " + encrypt);
					break;
				case EntryConfig.DETERMINISTIC_EQUIV:break;
				case EntryConfig.ORDER_LESS:
					long res;
					if(keysStored){
						long okey = (long)keys.get(i).getKey(0);
						HomoOpeInt ope = new HomoOpeInt(okey);
						res = ope.encrypt(auxInt);
						crypts.add(res);
					}else{
						long key = HomoOpeInt.generateKey();
						HomoOpeInt ope = new HomoOpeInt(key);
						res = ope.encrypt(auxInt);
						crypts.add(res);
						keys.get(i).addKey(key);
					}
					System.out.println("[ORDER_LESS]Encrypting: " + auxInt);
					System.out.println("[ORDER_LESS]Encrypted: " + res);
					break;
				case EntryConfig.ORDER_GREAT:break;
				case EntryConfig.ORDER_LESS_EQUAL:break;
				case EntryConfig.ORDER_GREAT_EQUAL:break;
				case EntryConfig.SEARCHABLE:
					String encrypted;
					if(keysStored){
						SecretKey skey = (SecretKey)keys.get(i).getKey(0); 
						encrypted = HomoSearch.encrypt(skey, auxString);
						crypts.add(encrypted);
					}else{
						SecretKey skey = HomoSearch.generateKey();
						encrypted = HomoSearch.encrypt(skey, auxString);
						crypts.add(encrypted);
						keys.get(i).addKey(skey);
					}
					System.out.println("[SEARCHABLE]Encrypting: " + auxString);
					System.out.println("[SEARCHABLE]Encrypted: " + encrypted);
					
					break;
				case EntryConfig.PAILIER:
					try{
						
						BigInteger big;
						BigInteger bigcrypt;
						if(keysStored){
							PaillierKey pkey = (PaillierKey)keys.get(i).getKey(0); 
							big = new BigInteger(Integer.toString(auxInt));
							bigcrypt = HomoAdd.encrypt(big, pkey);
							crypts.add(bigcrypt);
						}else{
							PaillierKey pk = HomoAdd.generateKey();
							big = new BigInteger(Integer.toString(auxInt));
							bigcrypt = HomoAdd.encrypt(big, pk);
							crypts.add(bigcrypt);
							keys.get(i).addKey(pk);
						}
						System.out.println("[PAILLIER]Encrypting: " + big.toString());
						System.out.println("[PAILLIER]Encrypted: " + bigcrypt.toString());
					
					}catch(Exception e){
						e.printStackTrace();
					}
					break;
				case EntryConfig.RSA:
					BigInteger bigOne;
					BigInteger bigCode;
					if(keysStored){
						RSAPublicKey rsaPublicKey = (RSAPublicKey)keys.get(i).getKey(0);
						RSAPrivateKey rsaPrivateKey = (RSAPrivateKey)keys.get(i).getKey(1); // not needed for now
						bigOne = new BigInteger(Integer.toString(auxInt));
						bigCode = HomoMult.encrypt(rsaPublicKey,bigOne);
						crypts.add(bigCode);
					}else{
						KeyPair keyPair = HomoMult.generateKey();
						RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
						RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
						bigOne = new BigInteger(Integer.toString(auxInt));
						bigCode = HomoMult.encrypt(rsaPublicKey,bigOne);
						crypts.add(bigCode);
						keys.get(i).addKey(rsaPublicKey);
						keys.get(i).addKey(rsaPrivateKey);
					}
					System.out.println("[RSA]Encrypting: " + bigOne.toString());
					System.out.println("[RSA]Encrypted: " + bigCode.toString());
					break;
				case EntryConfig.RAND:
					try{
						byte[] randEncrypt;
						if(keysStored){
							SecretKey randKey = (SecretKey) keys.get(i).getKey(0);
							byte[] iv = (byte[])keys.get(i).getKey(1);
							randEncrypt = HomoRand.encrypt(randKey, iv, auxString.getBytes("UTF-8"));
							crypts.add(randEncrypt);
						}else{
							SecretKey randKey = HomoRand.generateKey();
							byte[] iv = HomoRand.generateIV();
							randEncrypt = HomoRand.encrypt(randKey, iv, auxString.getBytes("UTF-8"));
							crypts.add(randEncrypt);
							keys.get(i).addKey(randKey);
							keys.get(i).addKey(iv);
						}
						System.out.println("[RAND]Encrypting: " + auxString);
						System.out.println("[RAND]Encrypted: " + randEncrypt);
					}catch(Exception e){
						e.printStackTrace();
					}
					break;
			
			}
			
		}
		keysStored = true;
		return new Entry(crypts.get(0),crypts.get(1),crypts.get(2),crypts.get(3),crypts.get(4),crypts.get(5));
		
}
	
	public Entry decryptEntry(Entry raw){
		
		Entry encryptedEntry = raw;
		List<Object> vals = new ArrayList<Object>(); //to store the uncripted values
		System.out.println("=====[DECRYPTING]=====");
		System.out.println("Encripted Entry:");
		System.out.println(encryptedEntry.values);
		System.out.println("Keys to Decript:");
		System.out.println(keys);
		System.out.println("Decripting Entry Values:");
		
		int size = encryptedEntry.values.size();
		for(int i = 0 ; i < size ; i++){
			
			switch(ops[i]){
			
			case EntryConfig.DETERMINISTIC_EQUAL:
				SecretKey dkey = (SecretKey)keys.get(i).getKey(0); //gets the KeyStorage for this operand and then get the Key
				String cryptDetVal = (String)encryptedEntry.getElem(i);
				String trueDetVal = HomoDet.decrypt(dkey, cryptDetVal);
				vals.add(trueDetVal);
				System.out.println("[DETER_EQUAL]Decrypted: " + trueDetVal);
				break;
			case EntryConfig.DETERMINISTIC_EQUIV:break;
			case EntryConfig.ORDER_LESS:
				long okey = (long)keys.get(i).getKey(0);
				HomoOpeInt ope = new HomoOpeInt(okey);
				long cryptOrdVal = (long) encryptedEntry.getElem(i);
				int trueOrdVal = ope.decrypt(cryptOrdVal);
				vals.add(trueOrdVal);
				System.out.println("[ORDER_LESS]Decrypted: " + trueOrdVal);
				break;
			case EntryConfig.ORDER_GREAT:break;
			case EntryConfig.ORDER_LESS_EQUAL:break;
			case EntryConfig.ORDER_GREAT_EQUAL:break;
			case EntryConfig.SEARCHABLE:
				SecretKey skey = (SecretKey)keys.get(i).getKey(0); 
				String cryptSerVal = (String)encryptedEntry.getElem(i);
				String trueSerVal = HomoSearch.decrypt(skey, cryptSerVal);
				vals.add(trueSerVal);
				System.out.println("[SEARCHABLE]Decrypted: " + trueSerVal);
				break;
			case EntryConfig.PAILIER:
				try{
					PaillierKey pkey = (PaillierKey)keys.get(i).getKey(0); 
					BigInteger cryptPaiVal = (BigInteger)encryptedEntry.getElem(i);
					BigInteger truePaiVal = HomoAdd.decrypt(cryptPaiVal, pkey);
					vals.add(truePaiVal);
					System.out.println("[PAILLIER]Decrypted: " + truePaiVal.toString());
				}catch(Exception e){
					e.printStackTrace();
				}
				break;
			case EntryConfig.RSA:
				RSAPublicKey pubKey = (RSAPublicKey)keys.get(i).getKey(0); //not needed for now
				RSAPrivateKey privKey = (RSAPrivateKey)keys.get(i).getKey(1);
				BigInteger cryptRSAVal = (BigInteger) encryptedEntry.getElem(i);
				BigInteger trueRSAVal = HomoMult.decrypt(privKey, cryptRSAVal);
				vals.add(trueRSAVal);
				System.out.println("[RSA]Decrypted: "+ trueRSAVal.toString() );
				break;
			case EntryConfig.RAND:
				//for Random, we need to store 2 keys
				SecretKey randKey = (SecretKey) keys.get(i).getKey(0);
				byte[] iv = (byte[])keys.get(i).getKey(1);
				byte[] cryptRandVal = (byte[]) encryptedEntry.getElem(i);
				String message = new String(HomoRand.decrypt(randKey, iv, cryptRandVal));
				vals.add(message);
				System.out.println("[RAND]Decrypted: "+ message);
				break;
		
			}
			
		}
		return new Entry(vals.get(0),vals.get(1),vals.get(2),vals.get(3),vals.get(4),vals.get(5));
	}
	
	public void load(){

		try{
			fr = new FileReader(filename);
			br = new BufferedReader(fr);
			
			//Read the Entry type structure
			String line = br.readLine();
			String[] typesAux = line.split(" ");
			types = new Object[typesAux.length];
			for(int i = 0 ; i < typesAux.length ; i++){
				if(typesAux[i].equalsIgnoreCase("int"))
					types[i] = new Integer(0);
				else
					types[i] = new String();
			}
			
			//Read the ops allowed on each column/values
			line = br.readLine();
			ops = line.split(" ");
			
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
}
