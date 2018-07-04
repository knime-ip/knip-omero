package org.knime.knip.omero2.port;

import java.net.URI;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.RemoteFileHandlerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * The connection information to connect to an omero server
 *
 * @author gabriel
 *
 */
public class OmeroConnectionInformation extends ConnectionInformation {

    private static final String OME = "ome";
    private static final String USE_ENCRYPTION_KEY = "use encryption";

    private static final long serialVersionUID = 1L;

    private boolean useEncryption;

    public OmeroConnectionInformation() {
        super();
        super.setProtocol(OME);
    }

    public boolean getUseEncryption() {
        return useEncryption;
    }

    public void setUseEncryption(final boolean useEncryption) {
        this.useEncryption = useEncryption;
    }

    @Override
    public String toString() {
        return toURI().toString();
    }

    @Override
    public void setProtocol(final String protocol) {
        if (OME.equals(protocol)) {
            super.setProtocol(protocol);
        } else {
            throw new UnsupportedOperationException("Not possible with OMERO");
        }
    }

    @Override
    public void save(final ModelContentWO model) {
        super.save(model);
        model.addBoolean(USE_ENCRYPTION_KEY, useEncryption);
    }

    public static OmeroConnectionInformation load(final ModelContentRO model)
            throws InvalidSettingsException {
        final OmeroConnectionInformation info = new OmeroConnectionInformation();
        info.setProtocol(model.getString("protocol"));
        info.setHost(model.getString("host"));
        info.setPort(model.getInt("port"));
        info.setUser(model.getString("user"));
        info.setPassword(model.getPassword("xpassword", "}l?>mn0am8ty1m<+nf"));
        info.setKeyfile(model.getString("keyfile"));
        info.setKnownHosts(model.getString("knownhosts"));
        info.setTimeout(model.getInt("timeout", 30000)); // new option in 2.10
        info.setUseKerberos(model.getBoolean("kerberos", false)); // new option
                                                                  // in 3.2
        info.setUseEncryption(model.getBoolean(USE_ENCRYPTION_KEY));
        return info;
    }

    @Override
    public String getProtocol() {
        return OME;
    }

    @Override
    public void fitsToURI(final URI uri) throws Exception {

        final String scheme = uri.getScheme().toLowerCase();
        if (!getProtocol().equals(scheme)) {
            throw new Exception("Protocol " + scheme
                    + " incompatible with connection information protcol " + getProtocol());
        }

        // Host
        final String uriHost = uri.getHost();
        if (uriHost == null) {
            throw new Exception("No host in URI " + uri);
        }
        if (!uriHost.toLowerCase().equals(getHost().toLowerCase())) {
            throw new Exception("Host incompatible. URI host: " + uriHost
                    + " connection information host " + getHost());
        }
        // Port
        int port = uri.getPort();
        // If port is invalid use default port
        port = port < 0 ? RemoteFileHandlerRegistry.getDefaultPort(scheme) : port;
        if (port != getPort()) {
            throw new Exception("Port incompatible");
        }
        // User
        final String user = uri.getUserInfo();
        // User might not be used
        if (user != null && getUser() != null && !user.equals(getUser())) {
            throw new Exception("User incompatible");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (useEncryption ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final OmeroConnectionInformation other = (OmeroConnectionInformation) obj;
        if (useEncryption != other.useEncryption)
            return false;
        return true;
    }
}
