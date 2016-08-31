package org.knime.knip.newomero.remote;

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
	public RemoteFile<OmeroConnection> createRemoteFile(URI uri, ConnectionInformation connectionInformation,
			ConnectionMonitor<OmeroConnection> connectionMonitor) throws Exception {

		String path = uri.getPath();
		if ("".equals(path) || "/".equals(path)) { //
			String name = MessageFormat.format("{0}@{1}", uri.getUserInfo(), uri.getHost());

			return new OmeroRemoteFile(uri, OmeroRemoteFileType.ROOT, 0l, connectionInformation, connectionMonitor,
					name);
		}

		String[] tokens = path.split("/");

		// tokens[0] is "" because of the leading / in the path
		OmeroRemoteFileType type = EnumUtils.valueForName(tokens[1], OmeroRemoteFileType.values());
		Long id = Long.parseLong(tokens[2]);

		// Parse the file type out of the uri

		return new OmeroRemoteFile(uri, type, id, connectionInformation, connectionMonitor, "file");
	}

}
