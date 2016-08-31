package org.knime.knip.newomero.port;

import java.util.Objects;

import javax.swing.JComponent;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObjectSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * Spec for the omero connection port object
 * 
 * @author gabriel
 *
 */
public class OmeroConnectionInformationPortObjectSpec extends ConnectionInformationPortObjectSpec {

	/**
	 * @noreference This class is not intended to be referenced by clients.
	 */
	public static final class Serializer
			extends AbstractSimplePortObjectSpecSerializer<OmeroConnectionInformationPortObjectSpec> {
	}

	private OmeroConnectionInformation omeroConnectionInfo;

	/**
	 * Create default port object spec without connection information.
	 */
	public OmeroConnectionInformationPortObjectSpec() {
		omeroConnectionInfo = null;
	}

	/**
	 * Create specs that contain connection information.
	 *
	 *
	 * @param connectionInformation
	 *            The content of this port object
	 */
	public OmeroConnectionInformationPortObjectSpec(final OmeroConnectionInformation omeroConnectionInfo) {
		if (omeroConnectionInfo == null) {
			throw new NullPointerException("List argument must not be null");
		}
		this.omeroConnectionInfo = omeroConnectionInfo;
	}

	/**
	 * Return the connection information contained by this port object spec.
	 *
	 *
	 * @return The content of this port object
	 */
	@Override
	public ConnectionInformation getConnectionInformation() {
		return omeroConnectionInfo;
	}

	public OmeroConnectionInformation getOmeroConnectionInformation() {
		return omeroConnectionInfo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JComponent[] getViews() {
		return new JComponent[] {};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object ospec) {
		if (ospec == this) {
			return true;
		}
		if (!(ospec instanceof OmeroConnectionInformationPortObjectSpec)) {
			return false;
		}
		OmeroConnectionInformationPortObjectSpec oCIPOS = (OmeroConnectionInformationPortObjectSpec) ospec;
		return Objects.equals(omeroConnectionInfo, oCIPOS.omeroConnectionInfo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return omeroConnectionInfo == null ? 0 : omeroConnectionInfo.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void save(final ModelContentWO model) {
		omeroConnectionInfo.save(model);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void load(final ModelContentRO model) throws InvalidSettingsException {
		omeroConnectionInfo = OmeroConnectionInformation.load(model);
	}

}
