package org.knime.knip.omero2.util;

import net.imagej.omero.OMEROCredentials;

import org.knime.knip.omero2.port.OmeroConnectionInformation;

public class OmeroUtils {

	private OmeroUtils() {
		// Utility class
	}

	/**
	 * Capitalizes the first letter of a String, ex: word -> Word.
	 * 
	 * @param word the word to capitalize.
	 * @return the capitalized word.
	 */
	public static String capitalize(String word) {
		return Character.toString(Character.toTitleCase(word.charAt(0))) + word.substring(1);
	}

	/**
	 * 
	 * Converts from {@link OmeroConnectionInformation} to {@link OMEROCredentials}
	 * (used in imagej-omero)
	 * 
	 * @param info the connection information to convert
	 * @return the resulting omero credentials
	 */
	public static OMEROCredentials convertToOmeroCredetials(final OmeroConnectionInformation info) {
		final OMEROCredentials creds = new OMEROCredentials();
		creds.setEncrypted(info.getUseEncryption());
		creds.setPassword(info.getPassword());
		creds.setUser(info.getUser());
		creds.setPort(info.getPort());
		creds.setServer(info.getHost());
		return creds;
	}
}
