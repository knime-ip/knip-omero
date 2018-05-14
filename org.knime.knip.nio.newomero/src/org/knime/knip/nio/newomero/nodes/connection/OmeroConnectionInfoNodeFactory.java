package org.knime.knip.nio.newomero.nodes.connection;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class OmeroConnectionInfoNodeFactory extends NodeFactory<OmeroConnectionInfoNodeModel> {

    @Override
    public OmeroConnectionInfoNodeModel createNodeModel() {
        return new OmeroConnectionInfoNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<OmeroConnectionInfoNodeModel> createNodeView(final int viewIndex,
            final OmeroConnectionInfoNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new OmeroConnectionInfoNodeDialog();
    }

}
