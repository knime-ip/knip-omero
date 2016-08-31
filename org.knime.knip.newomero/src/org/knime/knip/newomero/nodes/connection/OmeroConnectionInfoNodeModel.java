package org.knime.knip.newomero.nodes.connection;

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
import org.knime.knip.newomero.port.OmeroConnectionInformation;
import org.knime.knip.newomero.port.OmeroConnectionInformationPortObject;
import org.knime.knip.newomero.port.OmeroConnectionInformationPortObjectSpec;

public class OmeroConnectionInfoNodeModel extends NodeModel {

	private List<SettingsModel> settingsModels = new ArrayList<>();

	private SettingsModelString hostNameModel = OmeroConnectionInfoSettingsModels.createHostnameModel();
	private SettingsModelInteger portModel = OmeroConnectionInfoSettingsModels.createPortModel();
	private SettingsModelString usernameModel = OmeroConnectionInfoSettingsModels.createUsernameModel();
	private SettingsModelString passwordModel = OmeroConnectionInfoSettingsModels.createPasswordModel();
	private SettingsModelBoolean useEncryptionModel = OmeroConnectionInfoSettingsModels.createUseEncryptionModel();
	private SettingsModelBoolean useWorkflowCredentialsModel = OmeroConnectionInfoSettingsModels
			.createUseWorkflowCredentialsModel();
	private SettingsModelString workflowCredentialsModel = OmeroConnectionInfoSettingsModels
			.createWorkflowCredentialsModel();

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
		boolean usewfcred = useWorkflowCredentialsModel.getBooleanValue();
		usernameModel.setEnabled(!usewfcred);
		passwordModel.setEnabled(!usewfcred);
	}

	@Override
	protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {
		return new PortObject[] { new OmeroConnectionInformationPortObject(createSpec()) };
	}

	@Override
	protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return new PortObjectSpec[] { createSpec() };
	}

	private OmeroConnectionInformationPortObjectSpec createSpec() {
		OmeroConnectionInformation info = new OmeroConnectionInformation();
		info.setHost(hostNameModel.getStringValue());
		info.setPort(portModel.getIntValue());
		info.setUseEncryption(useEncryptionModel.getBooleanValue());

		if (useWorkflowCredentialsModel.getBooleanValue()) {
			ICredentials creds = getCredentialsProvider().get(workflowCredentialsModel.getStringValue());
			info.setUser(creds.getLogin());
			info.setPassword(creds.getPassword());
		} else {
			info.setUser(usernameModel.getStringValue());
			info.setPassword(passwordModel.getStringValue());
		}

		return new OmeroConnectionInformationPortObjectSpec(info);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		settingsModels.forEach(m -> m.saveSettingsTo(settings));
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		for (SettingsModel m : settingsModels) {
			m.validateSettings(settings);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		for (SettingsModel m : settingsModels) {
			m.loadSettingsFrom(settings);
		}
	}

	@Override
	protected void reset() {
		// not needed
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// not needed
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// not needed
	}
}
