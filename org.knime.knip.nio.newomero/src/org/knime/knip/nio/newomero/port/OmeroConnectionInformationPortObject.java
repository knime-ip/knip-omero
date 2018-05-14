package org.knime.knip.nio.newomero.port;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.util.ViewUtils;

/**
 * PortObject for Omero connections
 *
 * @author gabriel
 *
 */
public class OmeroConnectionInformationPortObject extends ConnectionInformationPortObject {

    public static final class Serializer
            extends AbstractSimplePortObjectSerializer<OmeroConnectionInformationPortObject> {
    }

    private OmeroConnectionInformationPortObjectSpec omeroConnectionInfoPOS;

    /**
     * Type of this port.
     */
    public static final PortType TYPE =
            PortTypeRegistry.getInstance().getPortType(ConnectionInformationPortObject.class);

    /**
     * Type of this optional port.
     */
    public static final PortType TYPE_OPTIONAL =
            PortTypeRegistry.getInstance().getPortType(ConnectionInformationPortObject.class, true);

    /**
     * Should only be used by the framework.
     */
    public OmeroConnectionInformationPortObject() {
        // Used by framework
    }

    /**
     * Creates a port object with the given connection information.
     *
     * @param connectionInformationPOS
     *            The spec wrapping the connection information.
     */
    public OmeroConnectionInformationPortObject(
            final OmeroConnectionInformationPortObjectSpec omeroConnectionPOS) {
        if (omeroConnectionPOS == null) {
            throw new NullPointerException("Argument must not be null");
        }
        final OmeroConnectionInformation connInfo =
                omeroConnectionPOS.getOmeroConnectionInformation();
        if (connInfo == null) {
            throw new NullPointerException("Connection information must be set (is null)");
        }
        omeroConnectionInfoPOS = omeroConnectionPOS;
    }

    @Override
    public ConnectionInformation getConnectionInformation() {
        return omeroConnectionInfoPOS.getConnectionInformation();
    }

    public OmeroConnectionInformation getOmeroConnectionInformation() {
        return omeroConnectionInfoPOS.getOmeroConnectionInformation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return omeroConnectionInfoPOS.getConnectionInformation().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        final JPanel f = ViewUtils.getInFlowLayout(new JLabel(getSummary()));
        f.setName("Connection");
        return new JComponent[] { f };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getSpec() {
        return omeroConnectionInfoPOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        // nothing to save; all done in spec, which is saved separately
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec)
            throws InvalidSettingsException, CanceledExecutionException {
        omeroConnectionInfoPOS = (OmeroConnectionInformationPortObjectSpec) spec;
    }

}
