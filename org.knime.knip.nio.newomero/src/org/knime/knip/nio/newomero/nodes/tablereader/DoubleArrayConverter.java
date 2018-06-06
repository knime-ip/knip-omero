package org.knime.knip.nio.newomero.nodes.tablereader;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.datacell.SimpleJavaToDataCellConverterFactory;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.scijava.util.DoubleArray;

public class DoubleArrayConverter extends SimpleJavaToDataCellConverterFactory<DoubleArray> {

	public DoubleArrayConverter() {
		super(DoubleArray.class, ListCell.getCollectionType(IntCell.TYPE), s -> {
			final List<DataCell> cells = new ArrayList<>(s.size());
			for (final double i : s) {
				cells.add(DoubleCellFactory.create(i));
			}
			return CollectionCellFactory.createListCell(cells);
		}, "org.scijava.util.DoubleArray-TO-ListCell<DoubleCell>");
	}
}
