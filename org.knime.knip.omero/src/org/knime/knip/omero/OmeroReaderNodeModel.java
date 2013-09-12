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
package org.knime.knip.omero;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.omero.insight.HeadlessImageLoader;
import org.openmicroscopy.shoola.env.data.login.UserCredentials;

/**
 * This is the model implementation of OmeroReader.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class OmeroReaderNodeModel extends NodeModel implements
		BufferedDataTableHolder {

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(OmeroReaderNodeModel.class);

	/** the constants for the connection speed. */
	/*
	 * To allow index based access in the model and dialog a static method is
	 * used to fill the array.
	 */
	private final String[] SPEED = OmeroReaderNodeDialog.createSpeedArray();

	// SETTINGS METHODS
	static SettingsModelString createServerSM() {
		return new SettingsModelString("OIserver", "");
	}

	static SettingsModelInteger createPortSM() {
		final int defaultPort = 4064;
		return new SettingsModelInteger("OIport", defaultPort);
	}

	static SettingsModelString createSpeedSM() {
		final String[] SPEED = OmeroReaderNodeDialog.createSpeedArray();
		final String defaultSpeed = SPEED[UserCredentials.MEDIUM];
		return new SettingsModelString("OIspeed", defaultSpeed);
	}

	static SettingsModelString createUserSM() {
		return new SettingsModelString("OIuser", "");
	}

	static SettingsModelString createPwSM() {
		return new SettingsModelString("OIpw", "");
	}

	// SETTINGS
	private final SettingsModelString m_serverSM = createServerSM();

	private final SettingsModelInteger m_portSM = createPortSM();

	private final SettingsModelString m_speedSM = createSpeedSM();

	private final SettingsModelString m_userSM = createUserSM();

	private final SettingsModelString m_pwSM = createPwSM();

	// SETTINGS VAR
	static final String IMAGE_ID_KEY = "ImageIDs";

	private Long[] m_pixelIDs;

	/* data table for table cell view */
	private BufferedDataTable m_data;

	/**
	 * Constructor for the node model.
	 */
	protected OmeroReaderNodeModel() {
		super(0, 1);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Connects to the OMERO database using user credentials from the settings
	 * models. Image loading is handled by an instance of
	 * {@link HeadlessImageLoader}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final HeadlessImageLoader planeLoader = new HeadlessImageLoader(
				getUserCredentials());
		final ImgPlusCellFactory cellFactory = new ImgPlusCellFactory(exec);
		final BufferedDataContainer con = exec
				.createDataContainer(createOutSpec());

		exec.setMessage("Connecting");

		planeLoader.connect();
		{
			Long[] imageIDs = planeLoader.getImageIDs(m_pixelIDs);

			final double rowPercent = m_pixelIDs.length / 100.0;
			final int totalRows = m_pixelIDs.length;
			int currentRow = 0;
			int successfullLoaded = 0;

			for (int i = 0; i < m_pixelIDs.length; i++) {
				long pixelID = m_pixelIDs[i];
				long imageID = imageIDs[i];

				exec.checkCanceled();
				// status message
				if (successfullLoaded == currentRow) {
					exec.setProgress(rowPercent * currentRow, "Reading image "
							+ (currentRow + 1) + "/" + totalRows);
				} else {
					exec.setProgress(rowPercent * currentRow, "Reading image "
							+ (currentRow + 1) + "/" + totalRows + " ("
							+ (currentRow - successfullLoaded) + " failed)");
				}

				// image loading and error handling
				@SuppressWarnings("rawtypes")
				ImgPlus<RealType> img = null;
				try {
					img = planeLoader.getImage(imageID, pixelID);
				} catch (final Exception e) {
					LOGGER.error("could not load image " + imageID + " / "
							+ pixelID);
					LOGGER.warn("Loading image  " + imageID + " / " + pixelID
							+ " caused the following exception: \n"
							+ e.getMessage());
				}

				if (img != null) {
					con.addRowToTable(new DefaultRow(new RowKey("omero_id_"
							+ pixelID), cellFactory.createCell(img)));
					successfullLoaded++;
				}

				// next image
				currentRow++;
			}
		}
		planeLoader.disconnect();
		con.close();

		LOGGER.getLevel();

		final BufferedDataTable[] data = new BufferedDataTable[] { con
				.getTable() };
		m_data = data[0];
		return data;
	}

	/**
	 * @return an outputspec containing a single image column.
	 */
	private DataTableSpec createOutSpec() {
		final DataColumnSpecCreator creator = new DataColumnSpecCreator(
				"Image", ImgPlusCell.TYPE);
		return new DataTableSpec(creator.createSpec());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		m_data = null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Checks if the user credentials are complete => login is possible and if
	 * the user specified at least one image id that should be loaded.
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// connection
		if (m_serverSM.getStringValue().isEmpty()
				|| m_userSM.getStringValue().isEmpty()
				|| m_pwSM.getStringValue().isEmpty()
		// port is defined as 4064
		// speed is from a defined range
		) {
			LOGGER.warn("the connection settings are incomplete");
		}

		// id s
		if ((m_pixelIDs == null) || (m_pixelIDs.length == 0)) {
			LOGGER.warn("please select some image ids");
		}

		return new DataTableSpec[] { createOutSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_serverSM.saveSettingsTo(settings);
		m_portSM.saveSettingsTo(settings);
		m_speedSM.saveSettingsTo(settings);
		m_userSM.saveSettingsTo(settings);
		m_pwSM.saveSettingsTo(settings);

		String listValue = "";
		if ((m_pixelIDs != null) && (m_pixelIDs.length > 0)) {
			for (int i = 0; i < (m_pixelIDs.length - 1); i++) {
				listValue += m_pixelIDs[i] + ";";
			}
			listValue += m_pixelIDs[m_pixelIDs.length - 1];
		}
		settings.addString(IMAGE_ID_KEY, listValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_serverSM.loadSettingsFrom(settings);
		m_portSM.loadSettingsFrom(settings);
		m_speedSM.loadSettingsFrom(settings);
		m_userSM.loadSettingsFrom(settings);
		m_pwSM.loadSettingsFrom(settings);

		final String listValue = settings.getString(
				OmeroReaderNodeModel.IMAGE_ID_KEY, "");
		final StringTokenizer tk = new StringTokenizer(listValue, ";");
		final ArrayList<Long> ids = new ArrayList<Long>();

		while (tk.hasMoreTokens()) {
			ids.add(Long.valueOf(tk.nextToken()));
		}
		m_pixelIDs = ids.toArray(new Long[] {});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_serverSM.validateSettings(settings);
		m_portSM.validateSettings(settings);
		m_speedSM.validateSettings(settings);
		m_userSM.validateSettings(settings);
		m_pwSM.validateSettings(settings);
		m_pwSM.validateSettings(settings);

		final String listValue = settings.getString(
				OmeroReaderNodeModel.IMAGE_ID_KEY, "");
		final StringTokenizer tk = new StringTokenizer(listValue, ";");
		final ArrayList<Long> ids = new ArrayList<Long>();

		while (tk.hasMoreTokens()) {
			ids.add(Long.valueOf(tk.nextToken()));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

	/**
	 * @return UserCredentials build from the settings models. The
	 *         UserCredentials contain the decrypted clear text password => they
	 *         should never be saved or stored!
	 */
	private UserCredentials getUserCredentials() {
		final String server = m_serverSM.getStringValue();
		final Integer port = m_portSM.getIntValue();
		final String speedStr = m_speedSM.getStringValue();

		final String user = m_userSM.getStringValue();
		String pw = "";
		try {
			pw = DialogComponentPasswordField.decrypt(m_pwSM.getStringValue());
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

	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { m_data };
	}

	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		m_data = tables[0];
	}
}
