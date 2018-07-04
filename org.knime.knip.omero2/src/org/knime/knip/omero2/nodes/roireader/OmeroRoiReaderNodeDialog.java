
package org.knime.knip.omero2.nodes.roireader;

import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;

public class OmeroRoiReaderNodeDialog extends DefaultNodeSettingsPane {

	protected static final NodeLogger LOGGER = NodeLogger.getLogger(OmeroRoiReaderNodeDialog.class);

	private final SettingsModelColumnName m_columnNameModel = OmeroRoiReaderNodeModel.createColumnModel();

	@SuppressWarnings("unchecked")
	public OmeroRoiReaderNodeDialog() {

		setHorizontalPlacement(true);
		addDialogComponent(
				new DialogComponentColumnNameSelection(m_columnNameModel, "Image URI column", 1, URIDataValue.class));
	}

}
