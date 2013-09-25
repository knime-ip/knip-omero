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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.PlanarAccess;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;
import omero.ServerError;

import org.knime.knip.omero.omerojava.GatewayUtilsExcerpt;
import org.knime.knip.omero.omerojava.Plane1D;
import org.openmicroscopy.shoola.env.Container;
import org.openmicroscopy.shoola.env.LookupNames;
import org.openmicroscopy.shoola.env.config.Registry;
import org.openmicroscopy.shoola.env.data.AdminService;
import org.openmicroscopy.shoola.env.data.DSAccessException;
import org.openmicroscopy.shoola.env.data.DSOutOfServiceException;
import org.openmicroscopy.shoola.env.data.FSAccessException;
import org.openmicroscopy.shoola.env.data.OmeroDataService;
import org.openmicroscopy.shoola.env.data.OmeroImageService;
import org.openmicroscopy.shoola.env.data.events.ConnectedEvent;
import org.openmicroscopy.shoola.env.data.events.ExitApplication;
import org.openmicroscopy.shoola.env.data.login.LoginService;
import org.openmicroscopy.shoola.env.data.login.UserCredentials;
import org.openmicroscopy.shoola.env.data.util.SecurityContext;
import org.openmicroscopy.shoola.env.event.AgentEvent;
import org.openmicroscopy.shoola.env.event.AgentEventListener;
import org.openmicroscopy.shoola.env.init.StartupException;

import pojos.ExperimenterData;
import pojos.ImageData;
import pojos.PixelsData;

/**
 * provides convenient methods to establish a headless connection to OMERO and
 * load images, or retriev information about images by their OMERO image IDs.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class HeadlessImageLoader implements AgentEventListener {

	/**
	 * contains all information needed to connect to a OMERO database.
	 * Initialized on construction.
	 */
	private final UserCredentials m_userCredentials;

	/** handle for the connection to OMERO. */
	private Container m_container;

	/** the current state of the connection. */
	private boolean m_isConnected;

	/** group id of the logged in experimenter. */
	private long m_groupID;

	/**
	 * @param uc
	 *            {@link HeadlessImageLoader#m_userCredentials
	 *            m_userCredentials}
	 */
	public HeadlessImageLoader(final UserCredentials uc) {
		m_userCredentials = uc;
	}

	// Insight Event Listener

	@Override
	public void eventFired(final AgentEvent e) {
		if ((e instanceof ExitApplication) || (e instanceof ConnectedEvent)) {
			m_isConnected = false;
		}
	}

	// End Insight Event Listener

	/**
	 * connects headless to OMERO. On success sets
	 * {@link HeadlessImageLoader#m_isConnected m_isConnected}
	 *
	 * @return the connection state
	 * @throws StartupException
	 */
	public boolean connect() throws StartupException {
		m_isConnected = connectPriv();
		return m_isConnected;
	}

	/**
	 * closes the OMERO connection and adjusts.
	 * {@link HeadlessImageLoader#m_isConnected m_isConnected}
	 */
	public void disconnect() {
		if (m_isConnected) {
			m_isConnected = false;
			final ExitApplication ev = new ExitApplication(false);
			ev.setSecurityContext(new SecurityContext(m_groupID));
			m_container.getRegistry().getEventBus().post(ev);
		}
	}

	/**
	 * @return the current state of the connection to OMERO.
	 */
	public boolean isConnected() {
		return m_isConnected;
	}

	/**
	 * Retriev ImageIDs from PixelIDs
	 *
	 * @param m_imageIDs
	 *            list of OMERO pixelIDs
	 * @return the imageIDs that belong to these pixelIDs
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	public Long[] getImageIDs(final Long[] m_imageIDs)
			throws DSOutOfServiceException, DSAccessException {
		if (!m_isConnected) {
			throw new DSOutOfServiceException("not connected");
		}

		ArrayList<Long> ids = new ArrayList<Long>();
		final OmeroImageService imgSvc = m_container.getRegistry()
				.getImageService();

		for (Long id : m_imageIDs) {
			PixelsData pix = imgSvc.loadPixels(new SecurityContext(m_groupID),
					id);
			ids.add(pix.getImage().getId());
		}

		return ids.toArray(new Long[] {});
	}

	/**
	 * loads a image with the specified ID from OMERO using the headless.
	 * connection
	 *
	 * @param imageID
	 *            specifies a image with its OMERO image ID
	 * @param pixelID
	 *            specifies a image with its OMERO pixel ID
	 * @return the image with the specified image ID
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 * @throws FSAccessException
	 * @throws ServerError
	 */
	@SuppressWarnings("rawtypes")
	public ImgPlus<RealType> getImage(final long imageID, final long pixelID)
			throws DSOutOfServiceException, DSAccessException,
			FSAccessException, ServerError {

		if (!m_isConnected) {
			throw new DSOutOfServiceException("not connected");
		}

		// retrieve dimensionality
		final LinkedList<Long> idList = new LinkedList<Long>();
		idList.add(imageID);
		final int[] dimLengths = getDimensions(idList)[0];

		// retrieve type
		final String typeString = getImageTypes(idList)[0];

		// load pixel data
		final Img<RealType> img = assembleImage(pixelID, typeString, dimLengths);

		return new ImgPlus<RealType>(img, "" + pixelID,
				OmeroKnimeConversionHelper.getAxes());
	}

	/**
	 * retrieves the length of the 5 OMERO dimensions for multiple images.
	 * specified by their OMERO image IDs
	 *
	 * @param imageIDs
	 *            list of OMERO pixel IDs
	 * @return {image}{X,Y,Z,T,C}
	 * @throws DSAccessException
	 * @throws DSOutOfServiceException
	 */
	public int[][] getDimensions(final List<Long> imageIDs)
			throws DSOutOfServiceException, DSAccessException {

		if (!m_isConnected) {
			throw new DSOutOfServiceException("not connected");
		}
		final OmeroDataService dataSvc = m_container.getRegistry()
				.getDataService();

		@SuppressWarnings("rawtypes")
		final Set images = dataSvc.getImages(new SecurityContext(m_groupID),
				ImageData.class, imageIDs, -1);

		final int[][] ret = new int[images.size()][5];

		int i = 0;
		for (final Object obj : images) {
			final ImageData img = (ImageData) obj;

			ret[i][0] = img.getDefaultPixels().getSizeX();
			ret[i][1] = img.getDefaultPixels().getSizeY();
			ret[i][2] = img.getDefaultPixels().getSizeZ();
			ret[i][3] = img.getDefaultPixels().getSizeT();
			ret[i][4] = img.getDefaultPixels().getSizeC();
			i++;
		}

		return ret;
	}

	/**
	 * retrieves the image types of a list of images specified by their OMERO
	 * image ID.
	 *
	 * @param imageIDs
	 *            list of OMERO image IDs
	 * @return list of string identifier representing the image types
	 *         {@link org.knime.knip.omero.omerojava.PixelTypes PixelTypes} of
	 *         the specified images
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	public String[] getImageTypes(final List<Long> imageIDs)
			throws DSOutOfServiceException, DSAccessException {
		if (!m_isConnected) {
			throw new DSOutOfServiceException("not connected");
		}
		final OmeroDataService dataSvc = m_container.getRegistry()
				.getDataService();
		@SuppressWarnings("rawtypes")
		final Set images = dataSvc.getImages(new SecurityContext(m_groupID),
				ImageData.class, imageIDs, -1);

		final String[] ret = new String[images.size()];

		int i = 0;
		for (final Object obj : images) {
			final ImageData img = (ImageData) obj;
			ret[i] = img.getDefaultPixels().getPixelType();
			i++;
		}

		return ret;
	}

	/**
	 * loads an image planewise from OMERO and assembles the result to create an
	 * ImgLib image.
	 *
	 * @param imageID
	 *            the OMERO image ID of the image that should be assembled
	 * @param typeString
	 *            string identifier of a image type
	 *            {@link org.knime.knip.omero.omerojava.PixelTypes PixelTypes}
	 * @param dimLengths
	 *            length of the 5 OMERO dimensions
	 * @return the loaded image in ImgLib format
	 *
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 * @throws FSAccessException
	 * @throws ServerError
	 */
	@SuppressWarnings("rawtypes")
	private Img<RealType> assembleImage(final long imageID,
			final String typeString, final int[] dimLengths)
			throws DSOutOfServiceException, DSAccessException,
			FSAccessException, ServerError {

		if (!m_isConnected) {
			new DSOutOfServiceException("not connected");
		}
		final OmeroImageService imgSvc = m_container.getRegistry()
				.getImageService();

		// create image & get container
		final RealType type = OmeroKnimeConversionHelper.makeType(typeString);

		@SuppressWarnings("unchecked")
		final Img<RealType> img = new PlanarImgFactory().create(dimLengths,
				type);
		@SuppressWarnings("unchecked")
		final PlanarAccess<ArrayDataAccess<?>> planarAccess = (PlanarAccess<ArrayDataAccess<?>>) img;

		// populate planes
		final long[] dimL = new long[dimLengths.length];
		for (int i = 0; i < dimLengths.length; i++) {
			dimL[i] = dimLengths[i];
		}

		final Interval interval = new FinalInterval(dimL);
		final int[] zct = new int[3];
		Arrays.fill(zct, 1);
		for (int i = 0; i < zct.length; i++) {
			if ((i + 2) >= img.numDimensions()) {
				break;
			}
			zct[i] = (int) interval.dimension(i + 2);
		}

		final IntervalIterator ii = new IntervalIterator(zct);

		// open the selected planes
		final int[] pos = new int[ii.numDimensions()];
		while (ii.hasNext()) {
			ii.fwd();
			ii.localize(pos);

			final byte[] rawData = imgSvc.getPlane(new SecurityContext(
					m_groupID), imageID, pos[0], pos[1], pos[2]);
			final int no = IntervalIndexer.positionToIndex(pos, zct);

			final Plane1D p1d = GatewayUtilsExcerpt.getPlane1D(dimLengths[0],
					dimLengths[1], typeString, rawData);

			planarAccess.setPlane(no, OmeroKnimeConversionHelper
					.makeDataAccessArray(typeString, p1d));
		}

		return img;
	}

	/**
	 * connects headless to OMERO using the
	 * {@link HeadlessImageLoader#m_userCredentials m_userCredentials} and inits
	 * the {@link HeadlessImageLoader#m_container m_container}.
	 *
	 * @return true if successfully connected
	 * @throws StartupException
	 */
	private boolean connectPriv() throws StartupException {

		final String homeDir = ConfigLocator.locateConfigFileDir();
		if (homeDir != null) {
                m_container = Container.startupInHeadlessMode(homeDir, null,
                		LookupNames.KNIME);
			final Registry reg = m_container.getRegistry();
			final LoginService svc = (LoginService) reg
					.lookup(LookupNames.LOGIN);

			final int value = svc.login(m_userCredentials);

			if (value == LoginService.CONNECTED) {
				// Listen to selection
				reg.getEventBus().register(this, ExitApplication.class);
				reg.getEventBus().register(this, ConnectedEvent.class);

				// get group id
				final AdminService adminSvc = reg.getAdminService();
				final ExperimenterData exp = adminSvc.getUserDetails();
				m_groupID = exp.getDefaultGroup().getId();

				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}

	}

}
