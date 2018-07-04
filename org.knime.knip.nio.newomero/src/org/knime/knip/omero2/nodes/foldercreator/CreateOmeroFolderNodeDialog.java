package org.knime.knip.omero2.nodes.foldercreator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.omero2.nodes.foldercreator.CreateOmeroFolderSettings.TargetType;
import org.knime.knip.omero2.port.OmeroConnectionInformationPortObjectSpec;
import org.knime.knip.omero2.remote.OmeroConnection;
import org.knime.knip.omero2.util.DialogComponentJButton;

import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ProjectData;

public class CreateOmeroFolderNodeDialog extends DefaultNodeSettingsPane {

    protected static final NodeLogger LOGGER =
            NodeLogger.getLogger(CreateOmeroFolderNodeDialog.class);
    private final SettingsModelString targetTypeModel =
            CreateOmeroFolderSettings.createTargetTypeModel();
    private final SettingsModelString nameModel = CreateOmeroFolderSettings.createTargetNameModel();
    private final SettingsModelString descriptionModel =
            CreateOmeroFolderSettings.createDescriptionModel();
    private final SettingsModelString projectSelectionModel =
            CreateOmeroFolderSettings.createProjectSelectionModel();
    private final SettingsModelLong selectedProjectIdModel =
            CreateOmeroFolderSettings.createSelectedProjectModel();

    private ConnectionInformation connectionInformation;
    private final DialogComponentStringSelection projectSelection;

    private Map<String, Long> idMap;
    private final DialogComponentJButton refreshButton;
    private OmeroConnection connection;
    private final DialogComponentLabel statusText;

    public CreateOmeroFolderNodeDialog() {

        createNewGroup("Folder Properties");
        setHorizontalPlacement(true);
        final DialogComponentStringSelection targetTypeSelection =
                new DialogComponentStringSelection(targetTypeModel, "Folder Type",
                        EnumUtils.getStringCollectionFromToString(TargetType.values()));
        addDialogComponent(targetTypeSelection);
        targetTypeModel.addChangeListener(e -> refreshModelState());

        // FIXME is this correct?
        final FlowVariableModel fvm = createFlowVariableModel(nameModel);
        addDialogComponent(new DialogComponentString(nameModel, "Name", true, 18, fvm));
        setHorizontalPlacement(false);

        addDialogComponent(new DialogComponentMultiLineString(descriptionModel, "Description"));
        closeCurrentGroup();

        createNewGroup("Dataset options");
        setHorizontalPlacement(true);
        projectSelection = new DialogComponentStringSelection(projectSelectionModel, "Root Project",
                new String[] { "<ROOT>" });
        addDialogComponent(projectSelection);

        refreshButton = new DialogComponentJButton("Refresh Projects");
        refreshButton
                .addActionListener(a -> SwingUtilities.invokeLater(() -> recreateProjectList()));
        addDialogComponent(refreshButton);
        setHorizontalPlacement(false);

        statusText = new DialogComponentLabel("Not connected to OMERO");
        addDialogComponent(statusText);

    }

    private void refreshModelState() {
        // Enable / disable buttons
        final boolean enableProjectSelection =
                TargetType.DATASET.toString().equals(targetTypeModel.getStringValue());
        projectSelectionModel.setEnabled(enableProjectSelection);
        refreshButton.setButtonEnabled(enableProjectSelection);

        if (enableProjectSelection && idMap == null && connectionInformation != null) {
            SwingUtilities.invokeLater(() -> recreateProjectList());
        }
    }

    /**
     * Creates the list of available projects and fills in the project selection
     *
     * @return
     */
    private boolean recreateProjectList() {
        statusText.setText("Loading projects from server..");
        try {
            if (connection == null) {
                connection = new OmeroConnection(connectionInformation);
            }
            if (!connection.isOpen()) {
                connection.open();
            }

            final Gateway gw = connection.getGateway();
            final SecurityContext ctx = connection.getSecurtiyContext();
            final BrowseFacility browse = gw.getFacility(BrowseFacility.class);
            final Collection<ProjectData> projects = browse.getProjects(ctx);

            final String formatString = "{0}; id: {1}";

            idMap = new HashMap<>();
            for (final ProjectData p : projects) {
                final String key = MessageFormat.format(formatString, p.getName(), p.getId());
                idMap.put(key, p.getId());
            }
            idMap.put("<ROOT>", CreateOmeroFolderSettings.ROOT_ID);

        } catch (final Throwable e) {
            statusText.setText("Loading unsuccesfull");
            JOptionPane.showMessageDialog(new JFrame(), e.getMessage(), "Invalid settings",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        final List<String> keys = new ArrayList<>(idMap.keySet());
        Collections.sort(keys);

        projectSelection.replaceListItems(keys, null);
        statusText.setText("Projects successfully loaded.");
        return true;
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {

        if (specs[0] == null) {
            throw new NotConfigurableException("A connection is required!");
        }

        final OmeroConnectionInformationPortObjectSpec object =
                (OmeroConnectionInformationPortObjectSpec) specs[0];
        connectionInformation = object.getConnectionInformation();

        try {
            selectedProjectIdModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Could not load project selection settings:", e);
        }
    }

    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        if (idMap != null) {
            final Long id = idMap.get(projectSelectionModel.getStringValue());
            selectedProjectIdModel.setLongValue(id);
        }
        selectedProjectIdModel.saveSettingsTo(settings);
    }

    @Override
    public void onOpen() {
        // create project list if required and not already created
        refreshModelState();
    }

    @Override
    public void onClose() {
        if (connection == null) {
            return;
        }
        if (connection.isOpen()) {
            try {
                connection.close();
            } catch (final Exception e) {
                // NB. Don't need this
            }
        }
    }

}
