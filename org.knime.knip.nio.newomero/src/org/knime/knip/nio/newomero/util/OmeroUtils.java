package org.knime.knip.nio.newomero.util;

public class OmeroUtils {

	private OmeroUtils() {
		// Utility class
	}

	/**
	 * Capitalizes the first letter of a String, ex: word -> Word.
	 * 
	 * @param word
	 *            the word to capitalize.
	 * @return the capitalized word.
	 */
	public static String capitalize(String word) {
		return Character.toString(Character.toTitleCase(word.charAt(0))) + word.substring(1);
	}
}
