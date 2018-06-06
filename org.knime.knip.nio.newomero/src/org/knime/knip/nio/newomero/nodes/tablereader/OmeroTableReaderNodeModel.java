package org.knime.knip.nio.newomero.nodes.tablereader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMEROService;
import net.imagej.table.Column;
import net.imagej.table.Table;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.nio.NIOGateway;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformation;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformationPortObject;
import org.knime.knip.nio.newomero.util.OmeroUtils;
import org.scijava.util.DoubleArray;
import org.scijava.util.FloatArray;
import org.scijava.util.IntArray;

public class OmeroTableReaderNodeModel extends NodeModel {

	private final List<SettingsModel> m_settingsModels = new ArrayList<>();

	private final SettingsModelInteger m_tableIdModel = OmeroTableReaderNodeModel.createTableIdModel();

	private final OMEROService m_omeroService = NIOGateway.getService(OMEROService.class);

	private final JavaToDataCellConverterRegistry converters = JavaToDataCellConverterRegistry.getInstance();

	protected OmeroTableReaderNodeModel() {
		super(new PortType[] { OmeroConnectionInformationPortObject.TYPE }, new PortType[] { BufferedDataTable.TYPE });

		// store settings models
		m_settingsModels.add(m_tableIdModel);

	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

		// download the table
		final OmeroConnectionInformation info = ((OmeroConnectionInformationPortObject) inObjects[0])
				.getOmeroConnectionInformation();
		final OMEROCredentials creds = OmeroUtils.convertToOmeroCredetials(info);
 
		final Table<?, ?> table;
		try {
			table = m_omeroService.downloadTable(creds, m_tableIdModel.getIntValue());
		} catch (final IOException e) {
			throw new IOException("Table with the id '" + m_tableIdModel.getIntValue() + "' could not found!");
		}

		if (table.getRowCount() < 1) {
			throw new IllegalArgumentException("Input table is empty!");
		}

		// create the outputspecs and converters
		final int numcols = table.getColumnCount();
		final DataColumnSpec[] columnSpecs = new DataColumnSpec[numcols];
		final JavaToDataCellConverter[] typeConverters = new JavaToDataCellConverter[numcols];
		for (int i = 0; i < numcols; i++) {
			final String colName = table.getColumnHeader(i);
			final DataType type = getTypeForCol(table, i, typeConverters, exec);
			columnSpecs[i] = new DataColumnSpecCreator(colName, type).createSpec();
		}

		// convert cells & create output rows
		final BufferedDataContainer container = exec
				.createDataContainer(new DataTableSpecCreator().addColumns(columnSpecs).createSpec());

		final DataCell[] cells = new DataCell[numcols];
		for (int row = 0; row < table.getRowCount(); row++) {
			for (int col = 0; col < numcols; col++) {
				cells[col] = typeConverters[col].convert(table.get(col, row));
			}
			String id = table.getRowHeader(row);
			if (id == null) {
				id = "Row_" + row;
			}
			container.addRowToTable(new DefaultRow(id, cells));
		}
		container.close();
		return new PortObject[] { container.getTable() };
	}

	private DataType getTypeForCol(final Table<?, ?> table, final int col, final JavaToDataCellConverter[] convert,
			final ExecutionContext exec) {
		final Column<?> ijCol = table.get(col);
		final Class<?> type = ijCol.getType();
		final DataType outType;
		if (type.equals(Double.class)) {
			outType = DoubleCell.TYPE;
		} else if (type.equals(Boolean.class)) {
			outType = BooleanCell.TYPE;
		} else if (type.equals(Long.class)) {
			outType = LongCell.TYPE;
		} else if (type.equals(Integer.class)) {
			outType = IntCell.TYPE;
		} else if (type.equals(Short.class)) {
			outType = IntCell.TYPE;
		} else if (type.equals(String.class)) {
			outType = StringCell.TYPE;
		} else if (type.equals(IntArray.class)) {
			outType = ListCell.getCollectionType(IntCell.TYPE);
		} else if (type.equals(DoubleArray.class)) {
			outType = ListCell.getCollectionType(DoubleCell.TYPE);
		} // TODO Add other array types

		else {
			outType = DataType.getMissingCell().getType();
		}

		// get the first available converter
		convert[col] = converters.getConverterFactories(type, outType).iterator().next().create(exec);
		return outType;
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (inSpecs[0] == null) {
			throw new InvalidSettingsException("An Omero Connection is required!");
		}

		// we only know the composition of the table after downloading it
		return new PortObjectSpec[] { null };
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

	public static SettingsModelIntegerBounded createTableIdModel() {
		return new SettingsModelIntegerBounded("Table ID", 0, 0, Integer.MAX_VALUE);
	}
}
