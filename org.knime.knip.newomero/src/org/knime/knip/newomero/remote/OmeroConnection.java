package org.knime.knip.newomero.remote;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.Connection;
import org.knime.knip.newomero.port.OmeroConnectionInformation;

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

	public OmeroConnection(ConnectionInformation info) {
		gateway = new Gateway(new SimpleLogger()); // FIXME Better logger
		
		OmeroConnectionInformation oInfo = (OmeroConnectionInformation) info;

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
		ExperimenterData user = gateway.connect(creds);
		ctx = new SecurityContext(user.getGroupId());
	}

	@Override
	public boolean isOpen() {
		boolean connected;
		try {
			connected = gateway.isConnected() || gateway.isAlive(ctx);
		} catch (DSOutOfServiceException e) {
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
