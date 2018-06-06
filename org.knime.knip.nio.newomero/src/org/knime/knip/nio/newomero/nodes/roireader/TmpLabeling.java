package org.knime.knip.nio.newomero.nodes.roireader;

import net.imagej.omero.roi.OMERORealMask;

import omero.gateway.model.EllipseData;
import omero.gateway.model.LineData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PointData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.PolylineData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TextData;

/**
 * Wrapper class for {@link OMERORealMask}, that allows easy access to the
 * label.
 *
 */
public class TmpLabeling {

	private OMERORealMask mask;

	public OMERORealMask getMask() {
		return mask;
	}

	public String getLabel() {
		return label;
	}

	public long getId() {
		return id;
	}

	private String label;
	private long id;

	public TmpLabeling(OMERORealMask mask) {
		this.mask = mask;

		final ShapeData shape = mask.getShape();
		label = getTextFromShape(shape);
		this.id = shape.getId();
	}

	private String getTextFromShape(final ShapeData shape) {
		if (shape instanceof PointData) {
			return ((PointData) shape).getText();
		}
		if (shape instanceof RectangleData) {
			return ((RectangleData) shape).getText();
		}
		if (shape instanceof EllipseData) {
			return ((EllipseData) shape).getText();
		}
		if (shape instanceof PolygonData) {
			return ((PolygonData) shape).getText();
		}
		if (shape instanceof PolylineData) {
			return ((PolylineData) shape).getText();
		}
		if (shape instanceof LineData) {
			return ((LineData) shape).getText();
		}
		if (shape instanceof TextData) {
			return ((TextData) shape).getText();
		}
		if (shape instanceof MaskData) {
			return ((MaskData) shape).getText();
		}
		throw new IllegalArgumentException();
	}

}
