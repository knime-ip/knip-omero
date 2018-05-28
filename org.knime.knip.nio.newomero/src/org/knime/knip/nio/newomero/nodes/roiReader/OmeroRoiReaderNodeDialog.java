
package org.knime.knip.nio.newomero.nodes.roiReader;

import javax.swing.SwingUtilities;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformationPortObjectSpec;
import org.knime.knip.nio.newomero.remote.OmeroConnection;

import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.TableData;

public class OmeroRoiReaderNodeDialog extends DefaultNodeSettingsPane {

	protected static final NodeLogger LOGGER = NodeLogger.getLogger(OmeroRoiReaderNodeDialog.class);

	private final SettingsModelInteger m_imgIdModel = OmeroRoiReaderNodeModel.createImgIdModel();

	private ConnectionInformation m_connectionInformation;

	private final DialogComponentLabel statusText;

	private OmeroConnection connection;

	public OmeroRoiReaderNodeDialog() {

		setHorizontalPlacement(true);
		createNewGroup("Image Properties");
		DialogComponentNumber diaC = new DialogComponentNumber(m_imgIdModel, "Image ID", 1,
				createFlowVariableModel(m_imgIdModel));
		addDialogComponent(diaC);

		final DialogComponentButton checkButton = new DialogComponentButton("Check");
		addDialogComponent(checkButton);
		checkButton.addActionListener(e -> SwingUtilities.invokeLater(this::checkValidity));
		setHorizontalPlacement(false);

		statusText = new DialogComponentLabel("Click 'Check' to see if the table id is valid");
		addDialogComponent(statusText);
	}

	@Override
	public void onOpen() {
		statusText.setText("Click 'Check' to see if the image id is valid");
	}

	/**
	 * Checks if the table id is valid
	 */
	private void checkValidity() {
		statusText.setText("Checking table...");
		try {
			if (connection == null) {
				connection = new OmeroConnection(m_connectionInformation);
			}
			if (!connection.isOpen()) {
				connection.open();
			}
			final Gateway gw = connection.getGateway();
			final SecurityContext ctx = connection.getSecurtiyContext();
			final ROIFacility facility = gw.getFacility(ROIFacility.class);
			int numRois = facility.getROICount(ctx, m_imgIdModel.getIntValue());

			if (numRois > 0) {
				statusText.setText("Found " + numRois + " rois attached to the image.");
			} else {
				statusText.setText("No rois are attached to the image!");
			}

		} catch (final Exception e) {
			statusText.setText("Error communicating with the server: " + e.getMessage());
		}
	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {

		if (specs[0] == null) {
			throw new NotConfigurableException("A connection is required!");
		}

		final OmeroConnectionInformationPortObjectSpec object = (OmeroConnectionInformationPortObjectSpec) specs[0];
		m_connectionInformation = object.getConnectionInformation();
	}

}
