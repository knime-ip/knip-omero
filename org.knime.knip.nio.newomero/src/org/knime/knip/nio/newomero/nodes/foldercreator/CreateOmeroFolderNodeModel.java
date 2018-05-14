package org.knime.knip.nio.newomero.nodes.foldercreator;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.nio.newomero.nodes.foldercreator.CreateOmeroFolderSettings.TargetType;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformation;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformationPortObject;
import org.knime.knip.nio.newomero.remote.OmeroConnection;

import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.model.Dataset;
import omero.model.Project;
import omero.model.ProjectI;

public class CreateOmeroFolderNodeModel extends NodeModel {

    private final List<SettingsModel> settingsModels = new ArrayList<>();

    private final SettingsModelString targetTypeModel =
            CreateOmeroFolderSettings.createTargetTypeModel();
    private final SettingsModelString nameModel = CreateOmeroFolderSettings.createTargetNameModel();
    private final SettingsModelString descriptionModel =
            CreateOmeroFolderSettings.createDescriptionModel();
    private final SettingsModelLong selectedProjectModel =
            CreateOmeroFolderSettings.createSelectedProjectModel();

    private final SettingsModelString variableNameModel =
            CreateOmeroFolderSettings.createVariableNameModel();

    protected CreateOmeroFolderNodeModel() {
        super(new PortType[] { OmeroConnectionInformationPortObject.TYPE },
                new PortType[] { FlowVariablePortObject.TYPE });

        // store settings models
        settingsModels.add(targetTypeModel);
        settingsModels.add(nameModel);
        settingsModels.add(descriptionModel);
        settingsModels.add(selectedProjectModel);

    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec)
            throws Exception {

        final OmeroConnectionInformation info =
                ((OmeroConnectionInformationPortObject) inObjects[0])
                        .getOmeroConnectionInformation();

        final OmeroConnection connection = new OmeroConnection(info);
        connection.open();

        final Gateway gw = connection.getGateway();
        final SecurityContext ctx = connection.getSecurtiyContext();

        final TargetType type =
                EnumUtils.valueForName(targetTypeModel.getStringValue(), TargetType.values());

        long createdID;
        String typeURIfragement;
        switch (type) {
        case DATASET:
            createdID = createDataSet(gw, ctx);
            typeURIfragement = "dataset";
            break;
        case PROJECT:
            createdID = createProject(gw, ctx);
            typeURIfragement = "project";
            break;
        default:
            throw new IllegalStateException("Implementation Error!"); // FIXME
        }

        final String formatString = info.toString() + "/{0}/{1}";
        // publish created folder
        final String pathToCreated =
                MessageFormat.format(formatString, typeURIfragement, createdID);
        pushFlowVariableString(variableNameModel.getStringValue(), pathToCreated);

        return new PortObject[] { FlowVariablePortObject.INSTANCE };
    }

    private long createProject(final Gateway gw, final SecurityContext ctx)
            throws DSOutOfServiceException {
        final Project o = gw.getImportStore(ctx).addProject(nameModel.getStringValue(),
                descriptionModel.getStringValue());
        return o.getId().getValue();

    }

    private long createDataSet(final Gateway gw, final SecurityContext ctx)
            throws DSOutOfServiceException, DSAccessException {
        final String name = nameModel.getStringValue();
        final String description = descriptionModel.getStringValue();
        Project project;

        final long projectId = selectedProjectModel.getLongValue();
        if (projectId == CreateOmeroFolderSettings.ROOT_ID) {
            // create dummy project to register the dataset at the root
            project = new ProjectI(null, false);
        } else {
            project = new ProjectI(projectId, false);
        }

        final Dataset res = gw.getImportStore(ctx).addDataset(name, description, project);
        return res.getId().getValue();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        if (inSpecs[0] == null) {
            throw new InvalidSettingsException("An Omero Connection is required!");
        }

        return new PortObjectSpec[] { FlowVariablePortObjectSpec.INSTANCE };
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settingsModels.forEach(m -> m.saveSettingsTo(settings));
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (final SettingsModel m : settingsModels) {
            m.validateSettings(settings);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        for (final SettingsModel m : settingsModels) {
            m.loadSettingsFrom(settings);
        }
    }

    @Override
    protected void reset() {
        // not needed
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // not needed
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // not needed
    }
}
