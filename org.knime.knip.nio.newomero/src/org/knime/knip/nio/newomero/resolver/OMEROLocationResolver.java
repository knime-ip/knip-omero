package org.knime.knip.nio.newomero.resolver;

import java.net.URI;
import java.net.URISyntaxException;

import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMERODataType;
import net.imagej.omero.OMEROLocation;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformation;
import org.knime.knip.nio.resolver.AuthAwareResolver;
import org.scijava.io.location.AbstractLocationResolver;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationResolver;
import org.scijava.plugin.Plugin;

@Plugin(type = LocationResolver.class)
public class OMEROLocationResolver extends AbstractLocationResolver implements AuthAwareResolver {

	public OMEROLocationResolver() {
		super("ome");
	}

	@Override
	public Location resolve(final URI uri) throws URISyntaxException {
		return null;
	}

	@Override
	public Location resolveWithAuth(final URI uri, final ConnectionInformation info) {

		if (!(info instanceof OmeroConnectionInformation)) {
			throw new IllegalArgumentException("Wrong ConnectionInformation object!");
		}

		final OmeroConnectionInformation omeroInfo = (OmeroConnectionInformation) info;

		final OMEROCredentials creds = new OMEROCredentials();
		creds.setUser(omeroInfo.getUser());
		creds.setServer(omeroInfo.getHost());
		creds.setPassword(omeroInfo.getPassword());
		creds.setEncrypted(omeroInfo.getUseEncryption());

		final String[] path = uri.getPath().split("/");
		if (path.length != 3) {
			throw new IllegalArgumentException("Invalid path component of uri: " + path);
		}
		final OMERODataType dataType = typeFromName(path[1]);
		final long id = Long.parseLong(path[2]);
		return new OMEROLocation(creds, dataType, id);
	}

	private OMERODataType typeFromName(final String p) {
		switch (p) {
		case "image":
			return OMERODataType.IMAGE;
		case "table":
			return OMERODataType.TABLE;

		// TODO Extend with ROI!
		default:
			throw new IllegalArgumentException("Type '" + p + "' is not supported!");
		}
	}

}
