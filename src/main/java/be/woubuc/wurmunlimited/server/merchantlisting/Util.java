package be.woubuc.wurmunlimited.server.merchantlisting;

import com.wurmonline.server.items.Item;

import static com.wurmonline.shared.util.MaterialUtilities.getClientMaterialString;
import static com.wurmonline.shared.util.MaterialUtilities.getRarityString;

public class Util {
	
	/**
	 * Takes the weight of an item and formats it in a readable format
	 * @param weight The weight
	 * @return The formatted weight string
	 */
	public static String formatWeight(long weight) {
		weight = weight / 10;
		
		int kg = 0;
		while (weight >= 100) {
			weight -= 100;
			kg++;
		}
		
		return kg + "." + (weight < 10 ? "0" : "") + weight;
	}
	
	/**
	 * Formats an item name containing rarity and material labels, similar to client item names
	 * @param item The item
	 * @return The name
	 */
	public static String formatItemName(Item item) {
		StringBuilder name = new StringBuilder();
		
		final byte materialId = item.getMaterial();
		final byte rarity = item.getRarity();
		final String baseName = item.getName().trim();
		
		if (rarity > 0) {
			// Add rarity label before name
			upperCase(name, getRarityString(rarity));
			name.append(baseName);
		} else {
			// If the name has no prefix, capitalise the first letter
			upperCase(name, baseName);
		}
		
		final String materialName = getClientMaterialString(materialId, true);
		if (materialName != null && baseName.length() != 0 && !baseName.contains(materialName) && baseName.charAt(0) != '"') {
			name.append(", ").append(materialName);
		}
		
		return name.toString().trim();
	}
	
	/**
	 * Appends a string to a string builder and uppercases the first letter of that string
	 * @param sb       The string builder
	 * @param original The string to uppercase
	 */
	private static void upperCase(StringBuilder sb, String original) {
		sb.append(original.substring(0, 1).toUpperCase()).append(original.substring(1));
	}
}
