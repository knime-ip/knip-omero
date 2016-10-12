package org.knime.knip.newomero.nodes.connection;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.knime.base.filehandling.remote.connectioninformation.node.TestConnectionDialog;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.knip.newomero.port.OmeroConnectionInformation;

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

		createNewGroup("Manual Credentials");
		addDialogComponent(new DialogComponentString(usernameModel, "username", true, 15));
		addDialogComponent(new DialogComponentPasswordField(passwordModel, "password"));
		closeCurrentGroup();

		createNewGroup("");

		DialogComponentButton testConnectionButton = new DialogComponentButton("Test connection");
		addDialogComponent(testConnectionButton);
		testConnectionButton.addActionListener(new TestConnectionListener());

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

	private class TestConnectionListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			// Get frame
			Frame frame = null;
			Container container = getPanel().getParent();
			while (container != null) {
				if (container instanceof Frame) {
					frame = (Frame) container;
					break;
				}
				container = container.getParent();
			}
			try {
				// Get connection information to current settings
				final OmeroConnectionInformation connectionInformation = createConnectionInformation(
						getCredentialsProvider());
				// Open dialog
				new TestConnectionDialog(connectionInformation).open(frame);
			} catch (final InvalidSettingsException exc) {
				JOptionPane.showMessageDialog(new JFrame(), exc.getMessage(), "Invalid settings",
						JOptionPane.ERROR_MESSAGE);
			}
		}

		// create temporary connection information for use in the test
		// connection method
		private OmeroConnectionInformation createConnectionInformation(CredentialsProvider credentialsProvider)
				throws InvalidSettingsException {
			OmeroConnectionInformation info = new OmeroConnectionInformation();
			info.setProtocol("ome");
			info.setHost(hostnameModel.getStringValue());
			info.setPort(portModel.getIntValue());
			info.setUseEncryption(encryptModel.getBooleanValue());
			if (useCredentialsModel.getBooleanValue()) {
				ICredentials creds = credentialsProvider.get(credentialsModel.getStringValue());
				info.setUser(creds.getLogin());
				info.setPassword(creds.getPassword());
			} else {
				info.setUser(usernameModel.getStringValue());
				info.setPassword(passwordModel.getStringValue());
			}
			return info;
		}
	}
}
