package org.knime.knip.nio.newomero.nodes.roiReader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class OmeroRoiReaderNodeFactory extends NodeFactory<OmeroRoiReaderNodeModel> {

	@Override
	public OmeroRoiReaderNodeModel createNodeModel() {
		return new OmeroRoiReaderNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<OmeroRoiReaderNodeModel> createNodeView(int viewIndex, OmeroRoiReaderNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new OmeroRoiReaderNodeDialog();
	}

}
