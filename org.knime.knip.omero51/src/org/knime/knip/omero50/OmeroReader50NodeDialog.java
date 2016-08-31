/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.omero50;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.omero50.insight.InsightGuiBridge;
import org.knime.knip.omero50.insight.InsightGuiListener;
import org.openmicroscopy.shoola.env.data.login.UserCredentials;

/**
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class OmeroReader50NodeDialog extends NodeDialogPane implements
		InsightGuiListener {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(OmeroReader50NodeDialog.class);

	/** the constants for the connection speed. */
	/*
	 * To allow index based access in the model and dialog a static method is
	 * used to fill the array.
	 */
	private final String[] SPEED = createSpeedArray();

	// CREDENTIALS
	/**
	 * creates an array with string representations of OMERO connection speed
	 * parameters. Arrays should be created with this method in the dialog and
	 * the model to ensure that the indices are the same.
	 * 
	 * @return {Low, High, LAN}
	 */
	static String[] createSpeedArray() {
		final String[] ret = new String[3];
		ret[UserCredentials.LOW] = "Low";
		ret[UserCredentials.MEDIUM] = "High";
		ret[UserCredentials.HIGH] = "LAN";
		return ret;
	}

	private final JPanel m_credentialsPanel;

	// DialogComponents
	private DialogComponentString m_serverDC;

	private DialogComponentNumberEdit m_portDC;

	private DialogComponentStringSelection m_speedDC;

	private DialogComponentString m_userDC;

	private DialogComponentPasswordField m_pwDC;

	private DialogComponentBoolean m_encryptedConnectionDC;

	// GUI and IDs
	private final JPanel m_mainPanel;

	private JButton m_startInsightB;

	private JLabel m_messageL;

	private DefaultListModel<Long> m_idListModel;

	/**
	 * holds a reference to the class that controlls the OMEREO.insight viewer.
	 * Might be null if no viewer is associated.
	 */
	private InsightGuiBridge m_insightGui;

	/**
	 * New pane for configuring the OmeroConnector node.
	 */
	protected OmeroReader50NodeDialog() {
		m_credentialsPanel = initCredentialsPanel();
		m_mainPanel = initMainPanel();

		final JPanel tabPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		tabPanel.add(m_credentialsPanel, gbc);

		gbc.gridy++;
		gbc.weighty = 1.0;
		tabPanel.add(m_mainPanel, gbc);

		addTab("OMERO.insight", tabPanel);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		m_serverDC.saveSettingsTo(settings);
		m_portDC.saveSettingsTo(settings);
		m_speedDC.saveSettingsTo(settings);
		m_userDC.saveSettingsTo(settings);
		m_encryptedConnectionDC.saveSettingsTo(settings);

		// encryption test
		if (!isEncryptionOnline()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JOptionPane
							.showMessageDialog(
									null,
									"OMERO Reader is unsafe!"
											+ "\n"
											+ "Encryption is not activated. Please set a Master Key"
											+ "(Preferences -> KNIME -> Master Key)",
									"OMERO Reader is unsafe!",
									JOptionPane.WARNING_MESSAGE);
				}
			});
			// encryption not running don't save pw
			((SettingsModelString) m_pwDC.getModel()).setStringValue("");
			m_pwDC.saveSettingsTo(settings);
		} else {
			// encryption is running save pw
			m_pwDC.saveSettingsTo(settings);
		}

		// save the selected image ids
		if (m_insightGui != null) {
			// close the connection before using the data
			if (m_insightGui != null) {
				m_insightGui.disconnect();
			}
			m_insightGui = null;
			m_startInsightB.setEnabled(true);
			m_messageL.setText("not connected");

		}

		String listValue = "";
		if (m_idListModel.size() > 0) {
			for (int i = 0; i < (m_idListModel.size() - 1); i++) {
				listValue += m_idListModel.elementAt(i) + ";";
			}
			listValue += m_idListModel.elementAt(m_idListModel.size() - 1);
		}
		settings.addString(OmeroReader50NodeModel.IMAGE_ID_KEY, listValue);

	}

	@Override
	public void onClose() {
		if (m_insightGui != null) {
			// close the connection before using the data
			if (m_insightGui != null) {
				m_insightGui.disconnect();
			}
			m_insightGui = null;
			m_startInsightB.setEnabled(true);
			m_messageL.setText("not connected");
		}
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		m_serverDC.loadSettingsFrom(settings, specs);
		m_portDC.loadSettingsFrom(settings, specs);
		m_speedDC.loadSettingsFrom(settings, specs);
		m_userDC.loadSettingsFrom(settings, specs);
		m_pwDC.loadSettingsFrom(settings, specs);
		m_encryptedConnectionDC.loadSettingsFrom(settings, specs);

		m_idListModel.clear();

		final String listValue = settings.getString(
				OmeroReader50NodeModel.IMAGE_ID_KEY, "");
		final StringTokenizer tk = new StringTokenizer(listValue, ";");

		while (tk.hasMoreTokens()) {
			m_idListModel.addElement(Long.parseLong(tk.nextToken()));
		}
	}

	@Override
	public void addImageIDs(final List<Long> imageIDs) {
		for (final Long l : imageIDs) {
			if (!m_idListModel.contains(l)) {
				m_idListModel.addElement(l);
			}
		}
	}

	@Override
	public void setMessage(final String message) {
		m_messageL.setText(message);
	}

	@Override
	public void terminated() {
		m_insightGui = null;
		m_startInsightB.setEnabled(true);
	}

	/**
	 * @return UserCredentials build from the settings models. The
	 *         UserCredentials contain the decrypted clear text password => they
	 *         should never be saved or stored!
	 */
	private UserCredentials getUserCredentials() {
		final String server = ((SettingsModelString) m_serverDC.getModel())
				.getStringValue();
		final Integer port = ((SettingsModelInteger) m_portDC.getModel())
				.getIntValue();
		final String speedStr = ((SettingsModelString) m_speedDC.getModel())
				.getStringValue();
		final String user = ((SettingsModelString) m_userDC.getModel())
				.getStringValue();

		String pw = "";
		try {
			pw = DialogComponentPasswordField
					.decrypt(((SettingsModelString) m_pwDC.getModel())
							.getStringValue());
		} catch (final Exception e) {
			LOGGER.error("password decryption failed");
		}

		int speed = UserCredentials.MEDIUM;
		if (speedStr.equals(SPEED[UserCredentials.LOW])) {
			speed = UserCredentials.LOW;
		} else if (speedStr.equals(SPEED[UserCredentials.HIGH])) {
			speed = UserCredentials.HIGH;
		}

		final UserCredentials uc = new UserCredentials(user, pw, server, speed);
		uc.setPort(port);

		return uc;
	}

	/**
	 * @return true if the encryption actually changes encrypted text aka a key
	 *         is set
	 */
	@SuppressWarnings("static-access")
	private boolean isEncryptionOnline() {
		// hacky test if the encryption works
		try {
			if (!m_pwDC.encrypt("is encryption online".toCharArray()).equals(
					"is encryption online")) {
				return true;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * @return a panel holding the image ID list field, a button to start
	 *         OMERO.insight and the remove selected entries button for the list
	 *         field.
	 */
	private JPanel initMainPanel() {
		final JPanel ret = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(5, 5, 0, 0);
		gbc.fill = GridBagConstraints.VERTICAL;

		final JLabel listL = new JLabel("IDs:");
		{
			gbc.gridx = 0;
			gbc.gridy = 0;
			ret.add(listL, gbc);
		}
		m_idListModel = new DefaultListModel<>();
		final JList<Long> list = new JList<>(m_idListModel);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);

		final JScrollPane listScroller = new JScrollPane(list);
		listScroller.setMinimumSize(new Dimension(150, 100));
		listScroller.setPreferredSize(new Dimension(150, 100));
		{
			gbc.gridx++;
			gbc.fill = GridBagConstraints.VERTICAL;
			gbc.weighty = 1.0;
			ret.add(listScroller, gbc);
		}
		final JPanel buttonPanel = new JPanel(new BorderLayout());
		final JPanel b2Panel = new JPanel();
		buttonPanel.add(b2Panel, BorderLayout.SOUTH);
		b2Panel.setLayout(new BoxLayout(b2Panel, BoxLayout.Y_AXIS));

		// INNER PART OF BUTTON PANEL
		m_startInsightB = new JButton("start OMERO.insight");
		m_startInsightB.addActionListener(e -> {
				if (m_insightGui == null) {
					m_startInsightB.setEnabled(false);
					m_insightGui = new InsightGuiBridge(
							OmeroReader50NodeDialog.this, getUserCredentials());

					final Thread t = new Thread(m_insightGui);
					t.start();
				}
			}
		);
		{
			b2Panel.add(m_startInsightB);
		}
		final JButton removeSelectedB = new JButton("remove selected");
		removeSelectedB.setMaximumSize(m_startInsightB.getMaximumSize());
		removeSelectedB.addActionListener(e -> list.getSelectedValuesList().forEach(value ->
            m_idListModel.removeElement(value)));
		{
			b2Panel.add(removeSelectedB);
		}
		// END BUTTON PANEL
		{
			gbc.gridx++;
			ret.add(buttonPanel, gbc);
		}
		m_messageL = new JLabel("not connected");
		m_messageL.setBackground(Color.white);
		{
			gbc.gridx = 0;
			gbc.gridy++;
			gbc.weighty = 0.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.anchor = GridBagConstraints.CENTER;
			ret.add(m_messageL, gbc);
		}

		return ret;
	}

	/**
	 * @return a panel holding input fields for the server, port, connection
	 *         speed, user name and password
	 */
	private JPanel initCredentialsPanel() {
		// SETTINGS
		final SettingsModelString serverSM = OmeroReader50NodeModel
				.createServerSM();
		final SettingsModelInteger portSM = OmeroReader50NodeModel.createPortSM();
		final SettingsModelString speedSM = OmeroReader50NodeModel
				.createSpeedSM();
		final SettingsModelString userSM = OmeroReader50NodeModel.createUserSM();
		final SettingsModelString pwSM = OmeroReader50NodeModel.createPwSM();
        final SettingsModelBoolean encryptedConnectionSM = OmeroReader50NodeModel.createEncryptedConnectionSM();

		// panel
		final JPanel ret = new JPanel(new GridBagLayout());
		ret.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.gray, 1),
				"User Credentials"));

		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(5, 5, 0, 0);

		m_serverDC = new DialogComponentString(serverSM, "Server:", true, 18);
		{
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets.right = 5;
			ret.add(m_serverDC.getComponentPanel(), gbc);
		}
		m_portDC = new DialogComponentNumberEdit(portSM, "Port:", 4);
		{
			gbc.gridx++;
			gbc.insets.right = 5;
			ret.add(m_portDC.getComponentPanel(), gbc);
		}

		m_speedDC = new DialogComponentStringSelection(speedSM, "", SPEED);
		{
			gbc.gridx++;
			gbc.insets.right = 5;
			ret.add(m_speedDC.getComponentPanel(), gbc);
		}
		{
			gbc.insets.top = 20;
		}
		m_userDC = new DialogComponentString(userSM, "Username:", true, 10);
		{
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.insets.right = 5;
			ret.add(m_userDC.getComponentPanel(), gbc);
		}
		{
			gbc.insets.top = 10;
			gbc.insets.bottom = 5;
		}
		m_pwDC = new DialogComponentPasswordField(pwSM, "Password:", 10);
		{
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.insets.right = 5;
			ret.add(m_pwDC.getComponentPanel(), gbc);
		}
		m_encryptedConnectionDC = new DialogComponentBoolean(encryptedConnectionSM, "Encrypted Connection");
		{
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.insets.right = 5;
			ret.add(m_encryptedConnectionDC.getComponentPanel(), gbc);
		}

		return ret;
	}

}
