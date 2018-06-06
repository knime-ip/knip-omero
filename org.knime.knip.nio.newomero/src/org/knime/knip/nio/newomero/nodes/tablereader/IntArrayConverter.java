package org.knime.knip.nio.newomero.nodes.tablereader;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.convert.datacell.SimpleJavaToDataCellConverterFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.scijava.util.IntArray;

public class IntArrayConverter extends SimpleJavaToDataCellConverterFactory<IntArray> {

	public IntArrayConverter() {
		super(IntArray.class, ListCell.getCollectionType(IntCell.TYPE), s -> {
			final List<DataCell> cells = new ArrayList<>(s.size());
			for (final int i : s) {
				cells.add(IntCellFactory.create(i));
			}
			return CollectionCellFactory.createListCell(cells);
		}, "org.scijava.util.IntArray-TO-ListCell<IntCell>");
	}
}
