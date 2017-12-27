package be.woubuc.wurmunlimited.server.merchantlisting;

import com.samskivert.mustache.Mustache;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;
import org.apache.commons.io.FilenameUtils;
import org.hashids.Hashids;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.logging.Logger;

public class RequestHandler implements HttpHandler {
	
	private final static Logger logger = Logger.getLogger(RequestHandler.class.getName());
	
	private final Hashids hashids;
	
	RequestHandler(String salt) {
		hashids = new Hashids(salt);
	}
	
	@Override
	public void handle(HttpExchange req) throws IOException {
		final String url = req.getRequestURI().toString();
		final String hashid = FilenameUtils.getBaseName(url);
		final String ext = FilenameUtils.getExtension(url);
		
		final long wurmId = hashids.decode(hashid)[0];
		
		// Prepare response
		String responseData = "";
		String contentType = "text/plain";
		int responseCode = 200;
		
		// Get merchant creature
		Creature merchant = null;
		try {
			merchant = Creatures.getInstance().getCreature(wurmId);
			if (!merchant.isSalesman()) throw new RuntimeException(); // Throw and catch to return a 404
		} catch (Exception e) {
			responseData = "Merchant not found";
			responseCode = 404;
		}
		
		if (merchant != null) {
			// Check which data type we should send (default to a HTML page)
			switch (ext) {
				case "json":
					responseData = getInventoryData(hashid, merchant).toJSONString();
					contentType = "application/json";
					break;
				default:
					InputStream template = getClass().getResourceAsStream("/template.html");
					if (template == null) throw new RuntimeException("Template not found");
					
					InputStreamReader reader = new InputStreamReader(template);
					responseData = Mustache.compiler().compile(reader).execute(getInventoryData(hashid, merchant));
					
					contentType = "text/html";
					break;
			}
		}
		
		// Send response data
		Headers headers = req.getResponseHeaders();
		headers.set("Content-Type", contentType);
		
		req.sendResponseHeaders(responseCode, responseData.getBytes().length);
		
		OutputStream res = req.getResponseBody();
		res.write(responseData.getBytes());
		res.close();
	}
	
	/**
	 * Gets a data object containing all inventory data from the merchant
	 * @param hashid The entered hashID
	 * @param merchant The merchant creature
	 * @return The inventory data object
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getInventoryData(String hashid, Creature merchant) {
		JSONObject data = new JSONObject();
		JSONArray items = new JSONArray();
		
		data.put("id", hashid);
		data.put("name", merchant.getName().substring(9));
		
		String village = null;
		if (merchant.getCurrentTile().getVillage() != null) {
			village = merchant.getCurrentTile().getVillage().getName();
		}
		data.put("village", village);
		
		data.put("x", (int) merchant.getPosX());
		data.put("y", (int) merchant.getPosY());
		
		for (Item item : merchant.getInventory().getItems()) {
			if (item.isCoin()) continue; // Coins should not show up in merchant inventory
			
			JSONObject itemData = new JSONObject();
			
			// Capitalise name cause it's prettier that way
			StringBuilder name = new StringBuilder();
			for (String word : item.getName().split("\\s")) {
				name.append(word.substring(0, 1).toUpperCase());
				name.append(word.substring(1));
				name.append(" ");
			}
			
			itemData.put("name", name.toString().trim());
			itemData.put("description", item.getDescription().length() == 0 ? null : item.getDescription());
			
			itemData.put("ql", item.getCurrentQualityLevel());
			itemData.put("dmg", item.getDamage());
			
			itemData.put("weight", formatWeight(item.getWeightGrams(true)));
			itemData.put("rawWeight", item.getWeightGrams(true));
			
			// Use the set price or fall back to the item's value
			long price = item.getPrice();
			if (price == 0) price = item.getValue();
			itemData.put("price", formatPrice(price));
			itemData.put("rawPrice", price);
			
			items.add(itemData);
		}
		data.put("inventory", items);
		
		return data;
	}
	
	/**
	 * Takes the weight of an item and formats it in a readable format
	 * @param weight The weight
	 * @return The formatted weight string
	 */
	private String formatWeight(long weight) {
		weight = weight / 10;
		
		int kg = 0;
		while (weight >= 100) {
			weight -= 100;
			kg++;
		}
		
		return kg + "." + (weight < 10 ? "0" : "") + weight;
	}
	
	/**
	 * Takes the price of an item and formats it in a readable format
	 * @param price The price
	 * @return The formatted price string
	 */
	private String formatPrice(long price) {
		
		int silver = 0;
		int iron = 0;
		
		while (price >= 1e5) {
			price -= 1e5;
			silver += 10;
		}
		
		while (price >= 1e4) {
			price -= 1e4;
			silver += 1;
		}
		
		iron = (int) price;
		return silver + "." + (iron < 10 ? "0" : "") + iron;
	}
}
