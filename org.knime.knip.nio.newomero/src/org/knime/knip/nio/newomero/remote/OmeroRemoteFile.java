package org.knime.knip.nio.newomero.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.knip.nio.newomero.port.OmeroConnectionInformation;
import org.knime.knip.nio.newomero.util.OmeroUtils;

import omero.api.IQueryPrx;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.TransferFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;
import omero.model.DatasetI;
import omero.model.IObject;
import omero.model.ImageI;

public class OmeroRemoteFile extends RemoteFile<OmeroConnection> {

	private final OmeroRemoteFileType type;
	private final Long id;
	private final NodeLogger log = NodeLogger.getLogger(getClass());

	private final String name;

	protected OmeroRemoteFile(final URI uri, final OmeroRemoteFileType type, final Long id,
			final ConnectionInformation connectionInformation,
			final ConnectionMonitor<OmeroConnection> connectionMonitor, final String name) {

		super(uri, connectionInformation, connectionMonitor);
		this.type = type;
		this.id = id;
		this.name = name;

	}

	@Override
	protected boolean usesConnection() {
		return true;
	}

	@Override
	protected OmeroConnection createConnection() {
		return new OmeroConnection(getConnectionInformation());
	}

	@Override
	public String getType() {
		return "ome";
	}

	public OmeroRemoteFileType getFileType() {
		return type;
	}

	@Override
	public boolean exists() throws Exception {
		return true; // TODO is this always the case?
	}

	@Override
	public boolean isDirectory() throws Exception {
		switch (type) {
		case IMAGE:
			return false;
		case DATASET:
			return true;
		case PROJECT:
			return true;
		case ROOT:
			return true;
		default:
			handleFailedMatch(type);
		}
		return false;
	}

	@Override
	public String getName() {
		if (type == OmeroRemoteFileType.ROOT) {
			return name;
		}
		// name (Type)
		return MessageFormat.format("{0} ({1}; id:{2,number,#})", name, OmeroUtils.capitalize(type.toString()), id);
	}

	@Override
	public String getPath() {
		String format = "";
		switch (type) {
		case IMAGE:
			format = "/image/{0,number,#}";
			break;
		case DATASET:
			format = "/dataset/{0,number,#}";
			break;
		case PROJECT:
			format = "/project/{0,number,#}";
			break;
		case ROOT:
			final URI uri = getURI();
			return uri.getUserInfo() + "@" + uri.getHost() + ":" + uri.getPort();
		default:
			handleFailedMatch(type);
		}
		return MessageFormat.format(format, this.id);
	}

	@Override
	public InputStream openInputStream() throws Exception {
		// open connection
		open();
		final Gateway gw = getConnection().getGateway();
		final SecurityContext ctx = getConnection().getSecurtiyContext();
		final TransferFacility transfer = gw.getFacility(TransferFacility.class);
		final File tempdir = FileUtil.createTempDir("omeroDownload-");
		final List<File> imgs = transfer.downloadImage(ctx, tempdir.getAbsolutePath(), getId());
		getConnection().close();

		if (imgs.size() != 1) {
			throw new IllegalStateException();
		}
		return new OmeroInputStream(imgs.get(0));
	}

	@Override
	public OutputStream openOutputStream() throws Exception {
		throw new UnsupportedOperationException(unsupportedMessage("open output stream"));
	}

	@Override
	public long getSize() throws Exception {
		throw new UnsupportedOperationException(unsupportedMessage("get size"));
	}

	@Override
	public long lastModified() throws Exception {
		throw new UnsupportedOperationException(unsupportedMessage("last modified"));
	}

	@Override
	public boolean delete() throws Exception {
		final Gateway gw = getConnection().getGateway();
		final SecurityContext ctx = getConnection().getSecurtiyContext();
		final BrowseFacility browse = gw.getFacility(BrowseFacility.class);
		final DataManagerFacility dm = gw.getFacility(DataManagerFacility.class);

		final List<Long> idList = Arrays.asList(id);

		try {
			IObject object = null;
			switch (type) {
			case IMAGE:
				object = browse.getImage(ctx, id).asIObject();
				break;
			case PROJECT:
				object = browse.getProjects(ctx, idList).iterator().next().asIObject();
				break;
			case DATASET:
				object = browse.getDatasets(ctx, idList).iterator().next().asIObject();
				break;
			case ROOT:
				throw new UnsupportedOperationException("Can not delete the root");
			}

			dm.delete(ctx, object);
		} catch (final NoSuchElementException e) {
			log.warn("Could not locate file on the server, maybe it has already been deleted?");
			return false;
		} catch (final Throwable e) {
			log.warn("Could not delete: " + e.getMessage());
			log.debug(e);
			return false;
		}
		return true;
	}

	@Override
	public RemoteFile<OmeroConnection>[] listFiles() throws Exception {
		final List<RemoteFile<OmeroConnection>> files = new ArrayList<>();

		if (getConnection() == null) {
			open();
		}

		final Gateway gw = getConnection().getGateway();
		final SecurityContext ctx = getConnection().getSecurtiyContext();
		final BrowseFacility browse = gw.getFacility(BrowseFacility.class);
		final IQueryPrx query = gw.getQueryService(ctx);

		final List<Long> idList = Arrays.asList(id);

		switch (type) {
		case IMAGE:
			break; // leave files empty
		case DATASET:
			// lists all images contained in a dataset, much faster than
			// browse.getImagesForDataset(..)

			final List<IObject> queryresults = query.findAllByQuery(//
					"select i from Image as i " //
							+ "left outer join fetch i.datasetLinks as links " //
							+ "join fetch links.parent as d where d.id = " + id,
					null);
			queryresults.forEach(i -> files.add(createFileFromImageI((ImageI) i)));
			break;
		case PROJECT:
			final ProjectData project = browse.getProjects(ctx, idList).iterator().next();
			final Set<DatasetData> datasets = project.getDatasets();
			datasets.forEach(dataset -> files.add(createFileFromDataset(dataset)));
			break;
		case ROOT:
			final Collection<ProjectData> projects = browse.getProjects(ctx);
			projects.forEach(p -> files.add(createFileFromProject(p)));

			// get datasets that are not in a project
			final List<IObject> orphanedDatsets = query.findAllByQuery("select d from Dataset as d "
					+ "left outer join fetch d.projectLinks as links " + "where links is null", null);
			orphanedDatsets.forEach(d -> files.add(createFileFromDatasetI((DatasetI) d)));

			break;
		default:
			throw new IllegalStateException("The datatype " + type.toString() + "is not supported");
		}
		Collections.sort(files);
		return files.toArray(new OmeroRemoteFile[files.size()]);
	}

	private RemoteFile<OmeroConnection> createFileFromProject(final ProjectData project) {
		final URI uri = createOMEROURI(OmeroRemoteFileType.PROJECT, 0l, getConnectionInformation());
		return new OmeroRemoteFile(uri, OmeroRemoteFileType.PROJECT, project.getId(), getConnectionInformation(),
				getConnectionMonitor(), project.getName());
	}

	private RemoteFile<OmeroConnection> createFileFromImageI(final ImageI image) {
		final long imgId = image.getId().getValue();
		final URI uri = createOMEROURI(OmeroRemoteFileType.IMAGE, imgId, getConnectionInformation());
		return new OmeroRemoteFile(uri, OmeroRemoteFileType.IMAGE, imgId, getConnectionInformation(),
				getConnectionMonitor(), image.getName().getValue());
	}

	private RemoteFile<OmeroConnection> createFileFromDataset(final DatasetData dataset) {
		final URI uri = createOMEROURI(OmeroRemoteFileType.DATASET, dataset.getId(), getConnectionInformation());
		return new OmeroRemoteFile(uri, OmeroRemoteFileType.DATASET, dataset.getId(), getConnectionInformation(),
				getConnectionMonitor(), dataset.getName());
	}

	private RemoteFile<OmeroConnection> createFileFromDatasetI(final DatasetI dataset) {
		final URI uri = createOMEROURI(OmeroRemoteFileType.DATASET, dataset.getId().getValue(),
				getConnectionInformation());
		return new OmeroRemoteFile(uri, OmeroRemoteFileType.DATASET, dataset.getId().getValue(),
				getConnectionInformation(), getConnectionMonitor(), dataset.getName().getValue());
	}

	private static URI createOMEROURI(final OmeroRemoteFileType type, final Long id, final ConnectionInformation info) {
		final OmeroConnectionInformation omeroInfo = (OmeroConnectionInformation) info;
		// ome://user@host:port/datatype/id
		final String path = MessageFormat.format("/{0}/{1,number,#}", type, id);

		URI uri = null;
		try {
			uri = new URI("ome", omeroInfo.getUser(), omeroInfo.getHost(), omeroInfo.getPort(), path, null, null);
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException("Could not create URI:", e);
		}
		return uri;
	}

	@Override
	public boolean mkDir() throws Exception {
		throw new UnsupportedOperationException(unsupportedMessage("mkdir"));
	}

	@Override
	/**
	 * Ordering between file types is: PROJECT > DATASET > IMAGE Within file types
	 * ordering is by id.
	 */
	public int compareTo(final RemoteFile<OmeroConnection> o) {
		final OmeroRemoteFile other = (OmeroRemoteFile) o;
		switch (other.getFileType()) {
		case DATASET:
			switch (getFileType()) {
			case DATASET:
				return id.compareTo(other.id);
			case IMAGE:
				return -1;
			case PROJECT:
				return 1;
			case ROOT:
				return 1;
			default:
				handleFailedMatch(getFileType());
			}
			break;
		case IMAGE:
			switch (getFileType()) {
			case DATASET:
				return 1;
			case IMAGE:
				return id.compareTo(other.id);
			case PROJECT:
				return 1;
			case ROOT:
				return 1;
			default:
				handleFailedMatch(getFileType());
			}
			break;
		case PROJECT:
			switch (getFileType()) {
			case DATASET:
				return 1;
			case IMAGE:
				return 1;
			case PROJECT:
				return id.compareTo(other.id);
			case ROOT:
				return 1;
			default:
				handleFailedMatch(getFileType());
			}
			break;
		case ROOT:
			switch (getFileType()) {
			case DATASET:
				return 1;
			case IMAGE:
				return 1;
			case PROJECT:
				return 1;
			case ROOT:
				return 0; // there is only one root
			default:
				handleFailedMatch(getFileType());
			}
			break;
		default:
			handleFailedMatch(other.getFileType());
		}
		throw new IllegalStateException("This should not have happend!");
	}

	private static void handleFailedMatch(final OmeroRemoteFileType datatype) {
		throw new IllegalStateException("The datatype " + datatype.toString() + "is not supported");
	}

	public Long getId() {
		return id;
	}

	private class OmeroInputStream extends FileInputStream {

		private final File file;

		/**
		 * @param file
		 * @throws FileNotFoundException
		 */
		public OmeroInputStream(final File file) throws FileNotFoundException {
			super(file);
			this.file = file;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() throws IOException {
			super.close();
			file.delete();
		}
	}
}
