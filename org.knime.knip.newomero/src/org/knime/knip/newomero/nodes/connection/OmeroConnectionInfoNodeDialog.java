package org.knime.knip.newomero.nodes.connection;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class OmeroConnectionInfoNodeDialog extends DefaultNodeSettingsPane {

	private final SettingsModelString hostnameModel = OmeroConnectionInfoSettingsModels.createHostnameModel();
	private final SettingsModelInteger portModel = OmeroConnectionInfoSettingsModels.createPortModel();
	private final SettingsModelBoolean encryptModel = OmeroConnectionInfoSettingsModels.createUseEncryptionModel();
	private SettingsModelBoolean useCredentialsModel = OmeroConnectionInfoSettingsModels
			.createUseWorkflowCredentialsModel();
	private SettingsModelString credentialsModel = OmeroConnectionInfoSettingsModels.createWorkflowCredentialsModel();
	private SettingsModelString usernameModel = OmeroConnectionInfoSettingsModels.createUsernameModel();
	private SettingsModelString passwordModel = OmeroConnectionInfoSettingsModels.createPasswordModel();

	private DialogComponentStringSelection credentialsComponent;
	private DialogComponentLabel errorLabel;

	public OmeroConnectionInfoNodeDialog() {

		createNewGroup("Server");
		addDialogComponent(new DialogComponentString(hostnameModel, "Hostname", true, 40));
		setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentNumber(portModel, "port", 1));
		addDialogComponent(new DialogComponentBoolean(encryptModel, "Encrypted Connection"));
		setHorizontalPlacement(false);
		closeCurrentGroup();

		createNewGroup("Workflow credentials");
		useCredentialsModel.addChangeListener(e -> {
			boolean usecreds = useCredentialsModel.getBooleanValue();
			usernameModel.setEnabled(!usecreds);
			passwordModel.setEnabled(!usecreds);
			credentialsModel.setEnabled(usecreds);
		});
		addDialogComponent(new DialogComponentBoolean(useCredentialsModel, "Use Workflow Credentials"));

		// Need to initialize dropdown menu with dummy as credentials are only
		// available when opening the node dialog
		List<String> dummyList = Arrays.asList(new String[] { "< NONE >" });
		credentialsComponent = new DialogComponentStringSelection(credentialsModel, "credentials", dummyList);
		addDialogComponent(credentialsComponent);
		errorLabel = new DialogComponentLabel("");

		createNewGroup("Username Password");
		addDialogComponent(new DialogComponentString(usernameModel, "username", true, 15));
		addDialogComponent(new DialogComponentPasswordField(passwordModel, "password"));
		closeCurrentGroup();

	}

	@Override
	public void onOpen() {
		// The ability to select workflow credentials is only available if there
		// are WFC!
		Collection<String> credentials = getCredentialsNames();
		if (credentials != null && !credentials.isEmpty()) {
			credentialsComponent.replaceListItems(getCredentialsNames(), credentialsModel.getStringValue());
			useCredentialsModel.setEnabled(true);
			if (useCredentialsModel.getBooleanValue()) {
				credentialsModel.setEnabled(true);
			} else {
				credentialsModel.setEnabled(false);
			}
		} else {
			useCredentialsModel.setEnabled(false);
			credentialsModel.setEnabled(false);
			passwordModel.setEnabled(true);
			usernameModel.setEnabled(true);

			errorLabel.setText("No workflowcredentials available!");
		}
	}

}
