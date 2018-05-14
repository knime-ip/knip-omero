package org.knime.knip.nio.newomero.nodes.foldercreator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class CreateOmeroFolderNodeFactory extends NodeFactory<CreateOmeroFolderNodeModel> {

	@Override
	public CreateOmeroFolderNodeModel createNodeModel() {
		return new CreateOmeroFolderNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	@Override
	public NodeView<CreateOmeroFolderNodeModel> createNodeView(int viewIndex,
			CreateOmeroFolderNodeModel nodeModel) {
		return null;
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new CreateOmeroFolderNodeDialog();
	}

}
