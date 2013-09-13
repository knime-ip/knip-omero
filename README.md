KNIME Image Processing OMERO Integration
==========

OMERO is a client-server software for visualisation, management and analysis of biological microscope images (http://www.openmicroscopy.org/). OMERO handles all your images in a secure central repository. You can view, organise, analyse and share your data from anywhere you have internet access.

It is planed to tightly integrate with OMERO. It is already possible to start the OMERO client [INSIGHT](http://www.openmicroscopy.org/site/support/omero4/users/client-tutorials/insight/getting-started.html) from within KNIME to easily import relevant images. After installing the OMERO plugin from the community update site the OMERO Reader node becomes available. The node stores the server path and credentials and allows to start the INSIGHT client to select images (rightclick -> View -> View in KNIME...). Once configured the reader fetches the selected images automatically during execution. The images can then be processed like images from the standard Image Reader.

Unfortunately writing to OMERO is not yet possible but we hope to further improve the integration of OMERO and provide bidirectional access to OMERO databases in the future.
