package auxiliary;

import Datatypes.Entry;

public class Test {

	
	public static void main(String[] args) {
		EntryConfig conf = new EntryConfig(EntryConfig.CONF_FILE);
		Entry dummy = new Entry(1,"two",3,"four",5,"six");
		Entry crypt = conf.encryptEntry(dummy);
		Entry uncrypt = conf.decryptEntry(crypt); //decrypt prints out results
		System.out.println(uncrypt);
	}
	

}
