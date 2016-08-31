package org.knime.knip.newomero.nodes.connection;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The class storing the configuration for the omero connection node
 * 
 * @author gabriel
 *
 */
public class OmeroConnectionInfoSettingsModels {

	private OmeroConnectionInfoSettingsModels() {
		// Utility class
	}

	protected static SettingsModelString createHostnameModel() {
		return new SettingsModelString("hostname", "localhost");
	}

	protected static SettingsModelInteger createPortModel() {
		return new SettingsModelInteger("port", 4064);
	}

	protected static SettingsModelString createUsernameModel() {
		return new SettingsModelString("username", "root");
	}

	protected static SettingsModelString createPasswordModel() {
		return new SettingsModelString("password", "");
	}

	protected static SettingsModelBoolean createUseEncryptionModel() {
		return new SettingsModelBoolean("use_encryption", true);
	}

	protected static SettingsModelBoolean createUseWorkflowCredentialsModel() {
		return new SettingsModelBoolean("use_workflow_credentials", false);
	}

	protected static SettingsModelString createWorkflowCredentialsModel() {
		return new SettingsModelString("workflow_credentials", "");
	}
}
