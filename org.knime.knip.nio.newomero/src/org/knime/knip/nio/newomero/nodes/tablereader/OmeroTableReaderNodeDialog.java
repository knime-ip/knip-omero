
package org.knime.knip.nio.newomero.nodes.tablereader;

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
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.TableData;

public class OmeroTableReaderNodeDialog extends DefaultNodeSettingsPane {

	private static final String CHECK_BUTTON_TXT = "Click 'Check' to see if the table ID is valid.";

	protected static final NodeLogger LOGGER = NodeLogger.getLogger(OmeroTableReaderNodeDialog.class);

	private final SettingsModelInteger m_tableIdModel = OmeroTableReaderNodeModel.createTableIdModel();

	private ConnectionInformation m_connectionInformation;

	private final DialogComponentLabel statusText;

	private OmeroConnection connection;

	public OmeroTableReaderNodeDialog() {

		setHorizontalPlacement(true);
		createNewGroup("Table Properties");
		DialogComponentNumber diaC = new DialogComponentNumber(m_tableIdModel, "Table ID", 1,
				createFlowVariableModel(m_tableIdModel));
		addDialogComponent(diaC);

		final DialogComponentButton checkButton = new DialogComponentButton("Check");
		addDialogComponent(checkButton);
		checkButton.addActionListener(e -> SwingUtilities.invokeLater(this::checkValidity));
		setHorizontalPlacement(false);

		statusText = new DialogComponentLabel(CHECK_BUTTON_TXT);
		addDialogComponent(statusText);
	}

	@Override
	public void onOpen() {
		statusText.setText(CHECK_BUTTON_TXT);
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
			final TablesFacility facility = gw.getFacility(TablesFacility.class);

			final TableData t = facility.getTableInfo(ctx, m_tableIdModel.getIntValue());
			if (t != null) {
				statusText.setText("Table " + m_tableIdModel.getIntValue() + " is valid, shape: " + t.getNumberOfRows()
						+ " rows and " + t.getColumns().length + " columns!");
			}

		} catch (final Exception e) {
			statusText.setText("Table: " + m_tableIdModel.getIntValue() + " was not found on the server!");
		}
	}

	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {

		if (specs[0] == null) {
			throw new NotConfigurableException("A connection is required, but not available!");
		}

		final OmeroConnectionInformationPortObjectSpec object = (OmeroConnectionInformationPortObjectSpec) specs[0];
		m_connectionInformation = object.getConnectionInformation();
	}

}
