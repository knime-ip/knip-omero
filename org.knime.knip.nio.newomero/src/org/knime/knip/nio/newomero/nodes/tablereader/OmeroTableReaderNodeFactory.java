package org.knime.knip.nio.newomero.nodes.tablereader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class OmeroTableReaderNodeFactory extends NodeFactory<OmeroTableReaderNodeModel> {

	@Override
	public OmeroTableReaderNodeModel createNodeModel() {
		return new OmeroTableReaderNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<OmeroTableReaderNodeModel> createNodeView(int viewIndex, OmeroTableReaderNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new OmeroTableReaderNodeDialog();
	}

}
