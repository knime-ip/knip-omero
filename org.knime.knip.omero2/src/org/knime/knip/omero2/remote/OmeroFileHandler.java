package org.knime.knip.omero2.remote;

import java.net.URI;
import java.text.MessageFormat;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.Protocol;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileHandler;
import org.knime.core.node.NodeLogger;
import org.knime.knip.core.util.EnumUtils;

public class OmeroFileHandler implements RemoteFileHandler<OmeroConnection> {

	public static final Protocol PROTOCOL = new Protocol("ome", 4064, false, false, false, true, true, false, true,
			false);

	NodeLogger log = NodeLogger.getLogger(getClass());

	public OmeroFileHandler() {
	}

	@Override
	public Protocol[] getSupportedProtocols() {
		return new Protocol[] { PROTOCOL };
	}

	@Override
	public RemoteFile<OmeroConnection> createRemoteFile(final URI uri,
			final ConnectionInformation connectionInformation,
			final ConnectionMonitor<OmeroConnection> connectionMonitor) throws Exception {

		final String path = uri.getPath();
		if ("".equals(path) || "/".equals(path)) { //
			final String name = MessageFormat.format("{0}@{1}", uri.getUserInfo(), uri.getHost());

			return new OmeroRemoteFile(uri, OmeroRemoteFileType.ROOT, 0l, connectionInformation, connectionMonitor,
					name);
		}

		final String[] tokens = path.split("/");

		// if (tokens.length == 2) { // Upload node tries to get parent folder
		// throw new UnsupportedOperationException(
		// "Uploading files is not supported, use the ImageWriter node to write
		// images to an Omero server.");
		// }

		if (tokens.length != 3) {
			throw new IllegalArgumentException("The supplied URI: \"" + uri + "\" is not valid. \n"
					+ "You are probably using a node which is not supported with OMERO");
		}

		// tokens[0] is "" because of the leading / in the path
		final OmeroRemoteFileType type = EnumUtils.valueForName(tokens[1], OmeroRemoteFileType.values());
		final Long id = Long.parseLong(tokens[2].replaceAll("[,\\.]", "")); // numbers can be comma or dot separated

		// Parse the file type out of the uri

		return new OmeroRemoteFile(uri, type, id, connectionInformation, connectionMonitor, "file");
	}

}
