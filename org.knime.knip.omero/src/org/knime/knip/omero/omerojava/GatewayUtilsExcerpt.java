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
package org.knime.knip.omero.omerojava;

import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.exception.DataSourceException;

/*
 * A part of the GatewayUtils class in the tools component of OMERO.
 * The two upper methods have been changed to better fit our available data.
 *
 * The only edited class in this package the other classes are copied directly from their origin.
 */

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class GatewayUtilsExcerpt {

	/**
	 * Extracts a 1D plane from the pixels set this object is working for.
	 *
	 * @param dimX
	 *            size of the x dimension
	 * @param dimY
	 *            size of the y dimension
	 * @param type
	 *            string identifier of the data type e.g. int8 (constants
	 *            defined in {@link PixelTypes})
	 * @param rawPlane
	 *            The raw bytes of the plane (z,c,t)
	 * @return A plane 1D object that encapsulates the actual plane pixels.
	 * @throws omero.ServerError
	 */
	public static Plane1D getPlane1D(final int dimX, final int dimY,
			final String type, final byte[] rawPlane) throws omero.ServerError {
		if (type == null) {
			throw new NullPointerException("type is null");
		}
		if (rawPlane == null) {
			throw new NullPointerException("rawPlane is null");
		}

		final int bytesPerPixels = getBytesPerPixels(type);
		final BytesConverter strategy = BytesConverter.getConverter(type);
		return createPlane1D(dimX, dimY, rawPlane, bytesPerPixels, strategy);
	}

	/**
	 * Convert the rawPlane data to a Plane1D object which can then convert that
	 * raw byte data to anytype the caller wants.
	 *
	 * @param dimX
	 *            size of the x dimension
	 * @param dimY
	 *            size of the y dimension
	 * @param rawPlane
	 *            The raw bytes of the plane (z,c,t)
	 * @param bytesPerPixel
	 *            The number of bytes per pixel.
	 * @param strategy
	 *            To transform bytes into pixels values.
	 * @return A plane 2D object that encapsulates the actual plane pixels.
	 * @throws DSAccessException
	 * @throws DSOutOfServiceException
	 * @throws DataSourceException
	 *             If an error occurs while retrieving the plane data from the
	 *             pixels source.
	 */
	private static Plane1D createPlane1D(final int dimX, final int dimY,
			final byte[] rawPlane, final int bytesPerPixels,
			final BytesConverter strategy) throws omero.ServerError {
		final ReadOnlyByteArray array = new ReadOnlyByteArray(rawPlane, 0,
				rawPlane.length);
		return new Plane1D(array, dimX, dimY, bytesPerPixels, strategy);
	}

	/**
	 * Returns the number of bytes per pixel depending on the pixel type.
	 *
	 * @param pixelsType
	 *            The pixels Type.
	 * @return See above.
	 */
	public static int getBytesPerPixels(final String pixelsType) {
		if (!PixelTypes.pixelMap.containsKey(pixelsType)) {
			throw new IllegalArgumentException(pixelsType
					+ " is not a valid PixelsType.");
		}
		return PixelTypes.pixelMap.get(pixelsType);
	}

}
