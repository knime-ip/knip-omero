package org.knime.knip.nio.newomero.nodes.roireader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.omero.OMEROSessionService;
import net.imagej.omero.roi.OMERORealMask;
import net.imagej.roi.ROITree;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.view.Views;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.data.img.LabelingMetadata;
import org.knime.knip.nio.NIOGateway;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformation;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformationPortObject;
import org.knime.knip.nio.newomero.util.OmeroUtils;
import org.scijava.convert.ConvertService;
import org.scijava.util.TreeNode;

import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;

public class OmeroRoiReaderNodeModel extends NodeModel {

	private static final int CONNECTION = 0;
	private static final int DATA = 1;

	private final List<SettingsModel> m_settingsModels = new ArrayList<>();

	private final SettingsModelColumnName m_columnModel = OmeroRoiReaderNodeModel.createColumnModel();

	private final OMEROService m_omeroService = NIOGateway.getService(OMEROService.class);
	private final OMEROSessionService m_sessions = NIOGateway.getService(OMEROSessionService.class);

	protected OmeroRoiReaderNodeModel() {
		super(new PortType[] { OmeroConnectionInformationPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });

		// store settings models
		m_settingsModels.add(m_columnModel);

	}

	@Override

	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

		final OmeroConnectionInformation info = ((OmeroConnectionInformationPortObject) inObjects[CONNECTION])
				.getOmeroConnectionInformation();
		final OMEROCredentials creds = OmeroUtils.convertToOmeroCredetials(info);

		final BufferedDataContainer container = exec.createDataContainer(createSpec());
		final LabelingCellFactory factory = new LabelingCellFactory(exec);

		final DataTable dataTable = (DataTable) inObjects[DATA];

		final int uriCellIdx = NodeUtils.autoColumnSelection(dataTable.getDataTableSpec(), m_columnModel,
				URIDataValue.class, OmeroRoiReaderNodeModel.class);

		// read rois

		for (final DataRow row : dataTable) {
			exec.checkCanceled();
			
			final URIDataValue uri = (URIDataValue) row.getCell(uriCellIdx);
			final String[] elems = uri.getURIContent().getURI().getPath().split("/");
			if (elems.length != 3) {
				throw new IllegalArgumentException("Invalid path at row: " + row.getKey());
			}

			final long id = Long.parseLong(elems[2]);

			final LabelingCell<String> cell = readRoi(id, creds, factory);
			container.addRowToTable(new DefaultRow(row.getKey(), cell));

		}

		// create output rows

		container.close();
		return new PortObject[] { container.getTable() };

	}

	private LabelingCell<String> readRoi(final long id, final OMEROCredentials creds, final LabelingCellFactory factory)
			throws IOException, DSOutOfServiceException, DSAccessException {

		// download ROI
		final ROITree roitree = m_omeroService.downloadROIs(creds, id);
		final OMEROSession session = m_sessions.getSession(creds);

		final ImageData image = session.browse().getImage(session.getSecurityContext(), id);
		final PixelsData defaultPixels = image.getDefaultPixels();

		// get the dimensions of the original image
		final long sizeX = defaultPixels.getSizeX();
		final long sizeY = defaultPixels.getSizeY();
		final long sizeZ = defaultPixels.getSizeZ();
		final long sizeT = defaultPixels.getSizeT();
		final long sizeC = defaultPixels.getSizeC();

		final List<TmpLabeling> masks = new ArrayList<>();
		final List<TreeNode<?>> children = roitree.children();
		for (final TreeNode<?> c : children) {
			final List<TreeNode<?>> c2 = c.children();
			for (final TreeNode<?> o : c2) {
				masks.add(new TmpLabeling((OMERORealMask) o.data()));
			}
		}

		final long[] dims = new long[] { sizeX, sizeY, sizeC, sizeT, sizeZ };
		final RandomAccessibleInterval<LabelingType<String>> res = Views
				.dropSingletonDimensions(KNIPGateway.ops().create().imgLabeling(new FinalInterval(dims)));

		final Cursor<LabelingType<String>> c = Views.iterable(res).localizingCursor();

		while (c.hasNext()) {
			c.next();
			for (final TmpLabeling m : masks) {
				if (m.getMask().test(c)) {
					String label = "" + m.getId();
					if (!m.getLabel().equals("")) {
						label += ":" + m.getLabel();
					}
					c.get().add(label);
				}
			}

		}
		final LabelingMetadata m = new DefaultLabelingMetadata(res.numDimensions(), new DefaultLabelingColorTable());
		return factory.createCell(res, m);

	}

	private DataTableSpec createSpec() {
		return new DataTableSpecCreator().addColumns(new DataColumnSpecCreator("ROIs", LabelingCell.TYPE).createSpec())
				.createSpec();
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (inSpecs[0] == null) {
			throw new InvalidSettingsException("An Omero Connection is required!");
		}

		// we only know the composition of the table after downloading it
		return new PortObjectSpec[] { createSpec() };
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_settingsModels.forEach(m -> m.saveSettingsTo(settings));
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel m : m_settingsModels) {
			m.validateSettings(settings);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel m : m_settingsModels) {
			m.loadSettingsFrom(settings);
		}
	}

	@Override
	protected void reset() {
		// not needed
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// not needed
	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// not needed
	}

	static SettingsModelColumnName createColumnModel() {
		return new SettingsModelColumnName("Image URIs", "");
	}
}
