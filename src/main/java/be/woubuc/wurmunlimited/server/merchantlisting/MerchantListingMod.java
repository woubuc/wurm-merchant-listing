package be.woubuc.wurmunlimited.server.merchantlisting;

import com.sun.net.httpserver.HttpServer;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import org.hashids.Hashids;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.logging.Logger;

public class MerchantListingMod implements WurmServerMod, Configurable, ServerStartedListener {
	
	public static Hashids hashids;
	
	private static final Logger logger = Logger.getLogger(MerchantListingMod.class.getName());
	
	private int port = 8080;
	private String hostname = "0.0.0.0";
	private String address = "http://127.0.0.1";
	
	private HttpServer server;
	
	@Override
	public void configure(Properties properties) {
		port = Integer.parseInt(properties.getProperty("port", Integer.toString(port)));
		hostname = properties.getProperty("hostname", hostname);
		address = properties.getProperty("address", address);
		final String salt = properties.getProperty("salt", "salt");
		
		if (salt.equals("salt")) {
			throw new RuntimeException("Using default salt value for merchant listing mod. Please configure a randomised custom salt in the properties file.");
		}
		MerchantListingMod.hashids = new Hashids(salt);
		
		if (address.equals("http://127.0.0.1")) {
			logger.warning("You are using the default public address configuration. This is a local loopback IP address that will not be usable by anyone outside of this machine.");
		}
	}
	
	@Override
	public void onServerStarted() {
		ModActions.registerAction(new GetListingAction(address, port));
		
		try {
			server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
			server.createContext("/", new RequestHandler());
			server.setExecutor(null);
			server.start();
		} catch (IOException e) {
			logger.severe("Could not start merchant listing server on port " + port);
		}
	}
}
