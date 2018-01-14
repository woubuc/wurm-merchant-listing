package be.woubuc.wurmunlimited.server.merchantlisting;

import com.samskivert.mustache.Mustache;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RequestHandler implements HttpHandler {
	
	private final DecimalFormat priceFormat;
	private final DecimalFormat numberFormat;
	
	RequestHandler() {
		final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator(',');
		priceFormat = new DecimalFormat("##0.00##", symbols);
		numberFormat = new DecimalFormat("#0.00", symbols);
	}
	
	@Override
	public void handle(HttpExchange req) throws IOException {
		final String url = req.getRequestURI().toString();
		final String hashid = FilenameUtils.getBaseName(url);
		final String ext = FilenameUtils.getExtension(url);
		
		final long wurmId = MerchantListingMod.hashids.decode(hashid)[0];
		
		// Prepare response
		String responseData = "";
		String contentType = "text/plain";
		int responseCode = 200;
		
		// Get merchant creature
		Creature merchant = null;
		try {
			merchant = Creatures.getInstance().getCreature(wurmId);
			if (!merchant.isNpcTrader()) throw new RuntimeException(); // Throw and catch to return a 404
		} catch (Exception e) {
			responseData = "Merchant not found";
			responseCode = 404;
		}
		
		// Get shop
		Shop shop = Economy.getEconomy().getShop(merchant);
		
		if (merchant != null) {
			// Check which data type we should send (default to a HTML page)
			switch (ext) {
				case "json":
					responseData = getInventoryData(hashid, merchant, shop).toJSONString();
					contentType = "application/json";
					break;
				default:
					InputStream template = getClass().getResourceAsStream("/template.html");
					if (template == null) throw new RuntimeException("Template not found");
					
					InputStreamReader reader = new InputStreamReader(template);
					responseData = Mustache.compiler().compile(reader).execute(getInventoryData(hashid, merchant, shop));
					
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
	 * @param  hashid    The entered hashID
	 * @param  merchant  The merchant creature
	 * @param  shop      The shop associated with the merchant
	 * @return The inventory data object
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getInventoryData(String hashid, Creature merchant, Shop shop) {
		JSONObject data = new JSONObject();
		JSONArray items = new JSONArray();
		
		List<Item> inventory = new ArrayList(merchant.getInventory().getItems());
		inventory.sort((a, b) -> (a.getName().compareTo(b.getName())));
		
		data.put("id", hashid);
		data.put("name", merchant.getName().substring(9));
		
		String village = null;
		if (merchant.getCurrentTile().getVillage() != null) {
			village = merchant.getCurrentTile().getVillage().getName();
		}
		data.put("village", village);
		
		data.put("x", (int) merchant.getPosX());
		data.put("y", (int) merchant.getPosY());
		
		for (Item item : inventory) {
			if (item.isCoin()) continue; // Coins should not show up in merchant inventory
			
			JSONObject itemData = new JSONObject();
			
			itemData.put("name", Util.formatItemName(item));
			itemData.put("description", item.getDescription().length() == 0 ? null : item.getDescription());
			itemData.put("templateId", item.getTemplateId());
			
			itemData.put("rarity", item.getRarity());
			itemData.put("ql", numberFormat.format(item.getQualityLevel()));
			itemData.put("dmg", numberFormat.format(item.getDamage()));
			
			itemData.put("weight", numberFormat.format(item.getWeightGrams(true) / 1000.0));
			itemData.put("rawWeight", item.getWeightGrams(true));
			
			// Use the set price or fall back to the item's value
			double price = item.getPrice();
			if (price == 0) price = Math.round(item.getValue() * shop.getPriceModifier());
			if (price < 2) price = 2;
			itemData.put("price", priceFormat.format(price / 10000));
			itemData.put("rawPrice", price);
			
			items.add(itemData);
		}
		data.put("inventory", items);
		
		return data;
	}
}
