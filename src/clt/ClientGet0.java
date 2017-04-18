package clt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ClientGet0 {
	static final File KEYSTORE = new File("./home/csd/client.jks");
	static final char[] JKS_PASSWORD = "changeit".toCharArray();
	static final char[] KEY_PASSWORD = "changeit".toCharArray();

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		
		Option hostnameOp = new Option("h", "hostname", true, "hostname adress");
		hostnameOp.setRequired(false);
        options.addOption(hostnameOp);
        
        Option keystoreOp = new Option("k", "keystore", true, "keystore path");
        keystoreOp.setRequired(false);
        options.addOption(keystoreOp);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ClientGet", options);

            System.exit(1);
            return;
        }
		
		String hostname = cmd.getOptionValue("hostname", "localhost:9090");
		File keystore = KEYSTORE;
		if( cmd.hasOption("keystore"))
			keystore = new File(cmd.getOptionValue("keystore"));

		SSLContext sslContext = SSLContext.getInstance("TLSv1");
		
		KeyStore keyStore = KeyStore.getInstance("JKS");
		try (InputStream is = new FileInputStream(keystore)) {
			keyStore.load(is, JKS_PASSWORD);
		}

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, KEY_PASSWORD);
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);

		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        
		Client client = ClientBuilder.newBuilder()
				.hostnameVerifier(new InsecureHostnameVerifier())
				.sslContext(sslContext)
				.build();

		URI baseURI = UriBuilder.fromUri("https://" + hostname + "/").build();
		WebTarget target = client.target(baseURI);

		String value = target.path("/server")
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get(String.class);

		System.err.println("Value: " + value);
	}

	static public class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
}
