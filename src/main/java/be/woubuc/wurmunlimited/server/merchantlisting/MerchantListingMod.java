package be.woubuc.wurmunlimited.server.merchantlisting;

import com.sun.net.httpserver.HttpServer;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.logging.Logger;

public class MerchantListingMod implements WurmServerMod, Configurable, ServerStartedListener {

	private static final Logger logger = Logger.getLogger(MerchantListingMod.class.getName());
	
	private int port = 8080;
	private String hostname = "0.0.0.0";
	private String salt = "salt";
	
	private HttpServer server;
	
	@Override
	public void configure(Properties properties) {
		port = Integer.parseInt(properties.getProperty("port", Integer.toString(port)));
		hostname = properties.getProperty("hostname", hostname);
		salt = properties.getProperty("salt", salt);
		
		if (salt.equals("salt")) {
			throw new RuntimeException("Using default salt value for merchant listing mod. Please configure a custom salt in the properties file.");
		}
	}
	
	@Override
	public void onServerStarted() {
		try {
			server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
			server.createContext("/merchants", new RequestHandler(salt));
			server.setExecutor(null);
			server.start();
		} catch (IOException e) {
			logger.severe("Could not start merchant listing server on port " + port);
		}
	}
}
