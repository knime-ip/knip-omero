package org.knime.knip.omero2.remote;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.Connection;
import org.knime.knip.omero2.port.OmeroConnectionInformation;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ExperimenterData;
import omero.log.SimpleLogger;

public class OmeroConnection extends Connection {

	private final LoginCredentials creds;
	private final Gateway gateway;
	private SecurityContext ctx;

	public OmeroConnection(final ConnectionInformation info) {
		gateway = new Gateway(new SimpleLogger()); // TODO Better logger

		final OmeroConnectionInformation oInfo = (OmeroConnectionInformation) info;

		creds = new LoginCredentials();
		creds.getServer().setHostname(oInfo.getHost());
		if (oInfo.getPort() > 0) {
			creds.getServer().setPort(oInfo.getPort());
		}

		creds.getUser().setUsername(oInfo.getUser());
		creds.getUser().setPassword(oInfo.getPassword());
		creds.setEncryption(oInfo.getUseEncryption());
	}

	@Override
	public void open() throws Exception {
		final ExperimenterData user = gateway.connect(creds);
		ctx = new SecurityContext(user.getGroupId());
	}

	@Override
	public boolean isOpen() {
		boolean connected;
		if (ctx == null) {
			return false;
		}
		try {
			connected = gateway.isConnected() || gateway.isAlive(ctx);
		} catch (final DSOutOfServiceException e) {
			return false;
		}
		return connected;
	}

	@Override
	public void close() throws Exception {
		gateway.disconnect();
	}

	/**
	 * @return the gateway used in this connection
	 */
	public Gateway getGateway() {
		return gateway;
	}

	/**
	 * @return the security context used in this connection
	 */
	public SecurityContext getSecurtiyContext() {
		return ctx;
	}

}
