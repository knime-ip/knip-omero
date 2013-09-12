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
package org.knime.knip.omero.insight;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openmicroscopy.shoola.env.Container;
import org.openmicroscopy.shoola.env.LookupNames;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.data.AdminService;
import org.openmicroscopy.shoola.env.data.events.ActivateAgents;
import org.openmicroscopy.shoola.env.data.events.ExitApplication;
import org.openmicroscopy.shoola.env.data.events.ViewInPluginEvent;
import org.openmicroscopy.shoola.env.data.login.LoginService;
import org.openmicroscopy.shoola.env.data.login.UserCredentials;
import org.openmicroscopy.shoola.env.data.util.SecurityContext;
import org.openmicroscopy.shoola.env.event.AgentEvent;
import org.openmicroscopy.shoola.env.event.AgentEventListener;

import pojos.DataObject;
import pojos.ExperimenterData;
import pojos.ImageData;

/**
 * Starts the OMERO.insight viewer with the provided UserCredentials then
 * listens to some events from the viewer.
 * 
 * The {@link InsightGuiBridge} is intended to run in its own thread and
 * provides methods for asynchronous communication with a Gui on the KNIME side.
 * These methods are specified in the {@link InsightGuiListener} interface.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class InsightGuiBridge implements Runnable, AgentEventListener {

	/**
	 * reference to a element on the KNIME side that is connected with the
	 * OMEREO.insight viewer via this class.
	 */
	private InsightGuiListener m_guiListener;

	/**
	 * contains all information needed to connect to a OMERO database.
	 * Initialized on construction.
	 */
	private final UserCredentials m_userCredentials;

	/** handle for the connection to OMERO. */
	private Container m_container;

	/**
	 * creates a new bridge between KNIME and a OMEREO.insight viewer.
	 * 
	 * @param guiListener
	 *            {@link m_guiListener}
	 * @param uc
	 *            {@link m_userCredentials}
	 */
	public InsightGuiBridge(final InsightGuiListener guiListener,
			final UserCredentials uc) {
		setGuiListener(guiListener);
		m_userCredentials = uc;
	}

	/**
	 * Starts the OMERO.insight viewer and tries to connect to the defined OMERO
	 * database using the specified user credentials. If the login attempt
	 * succeeds the {@link InsightGuiBridge} switches into a listener mode and
	 * forwards relevant interactions via the {@link InsightGuiListener}
	 * interface. If the login attempt fails basic error messages get forwarded.
	 */
	@Override
	public void run() {
		setListenerMessage("connecting");
		final String homeDir = ConfigLocator.locateConfigFileDir();
		if (homeDir != null) {

			m_container = Container.startupInHeadlessMode(homeDir, null,
					LookupNames.KNIME);
			setListenerMessage("starting OMERO.insight");

			final Registry reg = m_container.getRegistry();
			final LoginService svc = (LoginService) reg
					.lookup(LookupNames.LOGIN);

			setListenerMessage("starting login procedure");
			final int value = svc.login(m_userCredentials);

			if (value == LoginService.CONNECTED) {
				// start the UI if required.
				reg.getEventBus().post(new ActivateAgents());

				// Listen to selection
				reg.getEventBus().register(this, ViewInPluginEvent.class);
				reg.getEventBus().register(this, ExitApplication.class);

				setListenerMessage("connected");
			} else {
				setListenerMessage("login failed");
				informListenerTermination();
			}
		} else {
			setListenerMessage("could not find the plugin directory");
			informListenerTermination();
		}
	}

	/**
	 * listens to events from the OMERO.insight viewer.
	 * 
	 * @param e
	 *            an event from OMERO
	 */
	@Override
	public void eventFired(final AgentEvent e) {
		if (e instanceof ViewInPluginEvent) {
			final ViewInPluginEvent evt = (ViewInPluginEvent) e;
			if (evt.getPlugin() == LookupNames.KNIME) {
				// retrieve IDs of selected images
				final LinkedList<Long> ids = new LinkedList<Long>();

				for (final DataObject obj : evt.getDataObjects()) {
					if (obj instanceof ImageData) {
						ids.add(((ImageData) obj).getDefaultPixels().getId());
					}
				}

				if (ids.size() > 0) {
					informListenerSelection(ids);
				}
			}
		} else if (e instanceof ExitApplication) {
			m_guiListener.setMessage("not connected");
			informListenerTermination();
			m_guiListener = null;
		}
	}

	// Hopefully thread save communication between OMERO.insight and our dialog

	/**
	 * sets a new status message on the listener (
	 * {@link InsightGuiListener#setMessage(String)}). <br>
	 * <br>
	 * this method belongs to the synchronized helpers that control access to
	 * the gui listener to allow a connection shutdown from the KNIME side
	 * without possible null pointers
	 * 
	 * @param message
	 *            a string message that should be displayed to the user on a
	 *            KNIME component
	 */
	private synchronized void setListenerMessage(final String message) {
		if (m_guiListener != null) {
			m_guiListener.setMessage(message);
		}
	}

	/**
	 * informs the listener ({@link InsightGuiListener#terminated()}) that the
	 * connection is not longer available e.g. shutdown of the OMERO.insight
	 * viewer. <br>
	 * <br>
	 * this method belongs to the synchronized helpers that control access to
	 * the gui listener to allow a connection shutdown from the KNIME side
	 * without possible null pointers
	 */
	private synchronized void informListenerTermination() {
		if (m_guiListener != null) {
			m_guiListener.terminated();
		}
	}

	/**
	 * helper method to synchronize write access to the listener. <br>
	 * <br>
	 * this method belongs to the synchronized helpers that control access to
	 * the gui listener to allow a connection shutdown from the KNIME side
	 * without possible null pointers
	 * 
	 * @param guiListener
	 */
	private synchronized void setGuiListener(
			final InsightGuiListener guiListener) {
		m_guiListener = guiListener;
	}

	/**
	 * the image IDs are transfered to the registered listener (
	 * {@link InsightGuiListener#addImageIDs(List)}) if possible else (the
	 * listener is not longer registered) an error message is displayed. <br>
	 * <br>
	 * this method belongs to the synchronized helpers that control access to
	 * the gui listener to allow a connection shutdown from the KNIME side
	 * without possible null pointers
	 * 
	 * @param selectedImageIds
	 *            OMERO image ids
	 */
	private synchronized void informListenerSelection(
			final List<Long> selectedImageIds) {
		if (m_guiListener != null) {
			m_guiListener.addImageIDs(selectedImageIds);
		} else {
			JOptionPane.showMessageDialog(null,
					"sorry the connection to KNIME is broken", "no connection",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * shuts down the OMERO.insight viewer and terminates the connection between
	 * the bridge and the KNIME gui. <br>
	 * <br>
	 * this method belongs to the synchronized helpers that control access to
	 * the gui listener to allow a connection shutdown from the KNIME side
	 * without possible null pointers
	 */
	public synchronized void disconnect() {
		final Registry reg = m_container.getRegistry();

		m_guiListener = null;
		reg.getEventBus().remove(this);

		// logoff
		final AdminService adminSvc = reg.getAdminService();
		final ExperimenterData exp = adminSvc.getUserDetails();
		final long groupID = exp.getDefaultGroup().getId();

		final ExitApplication off = new ExitApplication(false);
		off.setSecurityContext(new SecurityContext(groupID));
		reg.getEventBus().post(off);
	}

}
