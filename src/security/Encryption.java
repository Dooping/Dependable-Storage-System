package security;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;

public class Encryption {
	
	static final File KEYSTORE = new File("./home/csd/server.jks");
	static final char[] JKS_PASSWORD = "changeit".toCharArray();
	static final char[] KEY_PASSWORD = "changeit".toCharArray();
	static final String ALIASPRIVATE = "server";
	static final String ALIASPUBLIC = "mykey";
	
	public static String Sign(String keystorePath, byte[] data) throws Exception{
		File keystore = KEYSTORE;
		if( keystorePath != null)
			keystore = new File(keystorePath);
		
		KeyStore keyStore = KeyStore.getInstance("JKS");
		try (InputStream is = new FileInputStream(keystore)) {
			keyStore.load(is, JKS_PASSWORD);
		}
		
		Key key = keyStore.getKey(ALIASPRIVATE, KEY_PASSWORD);
		
		Signature signature = Signature.getInstance("SHA256withRSA");
	    if (key instanceof PrivateKey) {
	    	signature.initSign((PrivateKey)key);
	    	signature.update(data);
	    	return Base64.getEncoder().encodeToString(signature.sign());
	    }
		
		return null;
	}
	
	public static boolean verifySign(String keystorePath, byte[] data, String sig, boolean own) throws Exception{
		File keystore = KEYSTORE;
		if( keystorePath != null)
			keystore = new File(keystorePath);
		
		KeyStore keyStore = KeyStore.getInstance("JKS");
		try (InputStream is = new FileInputStream(keystore)) {
			keyStore.load(is, JKS_PASSWORD);
		}
		
		Certificate certificate = (own) ? keyStore.getCertificate(ALIASPRIVATE) : keyStore.getCertificate(ALIASPUBLIC);
		
		Signature signature = Signature.getInstance("SHA256withRSA");
	    signature.initVerify(certificate);
	    signature.update(data);
		return signature.verify(Base64.getDecoder().decode(sig));
	}
	
}
