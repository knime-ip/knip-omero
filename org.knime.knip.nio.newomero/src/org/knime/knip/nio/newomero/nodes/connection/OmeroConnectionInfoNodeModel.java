package org.knime.knip.nio.newomero.nodes.connection;

import java.io.File;
import java.io.IOException;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ICredentials;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformation;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformationPortObject;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformationPortObjectSpec;

public class OmeroConnectionInfoNodeModel extends NodeModel {

    private final List<SettingsModel> settingsModels = new ArrayList<>();

    private final SettingsModelString hostNameModel =
            OmeroConnectionInfoSettingsModels.createHostnameModel();
    private final SettingsModelInteger portModel =
            OmeroConnectionInfoSettingsModels.createPortModel();
    private final SettingsModelString usernameModel =
            OmeroConnectionInfoSettingsModels.createUsernameModel();
    private final SettingsModelString passwordModel =
            OmeroConnectionInfoSettingsModels.createPasswordModel();
    private final SettingsModelBoolean useEncryptionModel =
            OmeroConnectionInfoSettingsModels.createUseEncryptionModel();
    private final SettingsModelBoolean useWorkflowCredentialsModel =
            OmeroConnectionInfoSettingsModels.createUseWorkflowCredentialsModel();
    private final SettingsModelString workflowCredentialsModel =
            OmeroConnectionInfoSettingsModels.createWorkflowCredentialsModel();

    protected OmeroConnectionInfoNodeModel() {
        super(new PortType[] {}, new PortType[] { OmeroConnectionInformationPortObject.TYPE });
        settingsModels.add(hostNameModel);
        settingsModels.add(passwordModel);
        settingsModels.add(portModel);
        settingsModels.add(useEncryptionModel);
        settingsModels.add(usernameModel);
        settingsModels.add(useWorkflowCredentialsModel);
        settingsModels.add(workflowCredentialsModel);

        // Enable / disable settingsmodels for consistency
        final boolean usewfcred = useWorkflowCredentialsModel.getBooleanValue();
        usernameModel.setEnabled(!usewfcred);
        passwordModel.setEnabled(!usewfcred);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec)
            throws Exception {
        return new PortObject[] { new OmeroConnectionInformationPortObject(createSpec()) };
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new PortObjectSpec[] { createSpec() };
    }

    private OmeroConnectionInformationPortObjectSpec createSpec() {
        final OmeroConnectionInformation info = new OmeroConnectionInformation();
        info.setHost(hostNameModel.getStringValue());
        info.setPort(portModel.getIntValue());
        info.setUseEncryption(useEncryptionModel.getBooleanValue());

        if (useWorkflowCredentialsModel.getBooleanValue()) {
            final ICredentials creds =
                    getCredentialsProvider().get(workflowCredentialsModel.getStringValue());
            info.setUser(creds.getLogin());
            info.setPassword(creds.getPassword());
        } else {
            info.setUser(usernameModel.getStringValue());
            info.setPassword(passwordModel.getStringValue());
        }

        return new OmeroConnectionInformationPortObjectSpec(info);
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
