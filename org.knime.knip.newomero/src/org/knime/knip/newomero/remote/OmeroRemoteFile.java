package org.knime.knip.newomero.remote;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.knip.newomero.port.OmeroConnectionInformation;
import org.knime.knip.newomero.util.OmeroUtils;

import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import omero.gateway.model.ProjectData;

public class OmeroRemoteFile extends RemoteFile<OmeroConnection> {

	private final OmeroRemoteFileType type;
	private final Long id;

	private String name;

	protected OmeroRemoteFile(URI uri, OmeroRemoteFileType type, Long id, ConnectionInformation connectionInformation,
			ConnectionMonitor<OmeroConnection> connectionMonitor, String name) {

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
		OmeroConnection conection = new OmeroConnection(getConnectionInformation());
		return conection;
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
		return MessageFormat.format("{0} ({1}; id:{2})", name, OmeroUtils.capitalize(type.toString()), id);
	}

	@Override
	public String getPath() {
		String format = "";
		switch (type) {
		case IMAGE:
			format = "/image/{0}";
			break;
		case DATASET:
			format = "/dataset/{0}";
			break;
		case PROJECT:
			format = "/project/{0}";
			break;
		case ROOT:
			return "/"; // only one root object
		default:
			handleFailedMatch(type);
		}
		return MessageFormat.format(format, this.id);
	}

	@Override
	public InputStream openInputStream() throws Exception {
		throw new UnsupportedOperationException(unsupportedMessage("open input stream"));
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
		throw new UnsupportedOperationException(unsupportedMessage("delete"));
	}

	@Override
	public RemoteFile<OmeroConnection>[] listFiles() throws Exception {
		final List<RemoteFile<OmeroConnection>> files = new ArrayList<>();

		if (getConnection() == null) {
			open();
		}

		final Gateway gw = getConnection().getGateway();
		final SecurityContext ctx = getConnection().getSecurtiyContext();
		BrowseFacility browse = gw.getFacility(BrowseFacility.class);

		List<Long> idList = Arrays.asList(id);

		switch (type) {
		case IMAGE:
			break; // leave files empty
		case DATASET:
			Collection<ImageData> images = browse.getImagesForDatasets(ctx, idList);
			images.forEach(i -> {
				files.add(createFileFromImg(i));
			});
			break;
		case PROJECT:
			ProjectData project = browse.getProjects(ctx, idList).iterator().next();
			Set<DatasetData> datasets = project.getDatasets();
			datasets.forEach(dataset -> {
				files.add(createFileFromDataset(dataset));
			});
			break;
		case ROOT:
			Set<Long> setIds = new HashSet<>();

			Collection<ProjectData> projects = browse.getProjects(ctx);
			projects.forEach(p -> {
				files.add(createFileFromProject(p));
				p.getDatasets().forEach(ds -> setIds.add(ds.getId()));
			});

			// ensure we only add datasets that are not children of a project
			for (DatasetData ds : browse.getDatasets(ctx)) {
				if (!setIds.contains(ds.getId())) {
					files.add(createFileFromDataset(ds));
				}
			}
			break;
		default:
			throw new IllegalStateException("The datatype " + type.toString() + "is not supported");
		}
		Collections.sort(files);
		return files.toArray(new OmeroRemoteFile[files.size()]);
	}

	private RemoteFile<OmeroConnection> createFileFromProject(ProjectData project) {
		URI uri = createOMEROURI(OmeroRemoteFileType.PROJECT, 0l, getConnectionInformation());
		return new OmeroRemoteFile(uri, OmeroRemoteFileType.PROJECT, project.getId(), getConnectionInformation(),
				getConnectionMonitor(), project.getName());
	}

	private RemoteFile<OmeroConnection> createFileFromImg(ImageData image) {
		URI uri = createOMEROURI(OmeroRemoteFileType.IMAGE, image.getId(), getConnectionInformation());
		return new OmeroRemoteFile(uri, OmeroRemoteFileType.IMAGE, image.getId(), getConnectionInformation(),
				getConnectionMonitor(), image.getName());
	}

	private RemoteFile<OmeroConnection> createFileFromDataset(DatasetData dataset) {
		URI uri = createOMEROURI(OmeroRemoteFileType.DATASET, dataset.getId(), getConnectionInformation());
		return new OmeroRemoteFile(uri, OmeroRemoteFileType.DATASET, dataset.getId(), getConnectionInformation(),
				getConnectionMonitor(), dataset.getName());
	}

	private static URI createOMEROURI(final OmeroRemoteFileType type, final Long id, ConnectionInformation info) {
		OmeroConnectionInformation omeroInfo = (OmeroConnectionInformation) info;
		// ome://user@host:port/datatype/id
		String path = MessageFormat.format("/{0}/{1}", type.toString(), id);

		URI uri = null;
		try {
			uri = new URI("ome", omeroInfo.getUser(), omeroInfo.getHost(), omeroInfo.getPort(), path, null, null);
		} catch (URISyntaxException e) {
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
	 * Ordering between file types is: PROJECT > DATASET > IMAGE Within file
	 * types ordering is by id.
	 */
	public int compareTo(RemoteFile<OmeroConnection> o) {
		OmeroRemoteFile other = (OmeroRemoteFile) o;
		switch (other.getFileType()) {
		case DATASET:
			switch (getFileType()) {
			case DATASET:
				return id.compareTo(other.id);
			case IMAGE:
				return 1;
			case PROJECT:
				return -1;
			case ROOT:
				return -1;
			default:
				handleFailedMatch(getFileType());
			}
			break;
		case IMAGE:
			switch (getFileType()) {
			case DATASET:
				return -1;
			case IMAGE:
				return id.compareTo(other.id);
			case PROJECT:
				return -1;
			case ROOT:
				return -1;
			default:
				handleFailedMatch(getFileType());
			}
			break;
		case PROJECT:
			switch (getFileType()) {
			case DATASET:
				return -1;
			case IMAGE:
				return -1;
			case PROJECT:
				return id.compareTo(other.id);
			case ROOT:
				return -1;
			default:
				handleFailedMatch(getFileType());
			}
			break;
		case ROOT:
			switch (getFileType()) {
			case DATASET:
				return -1;
			case IMAGE:
				return -1;
			case PROJECT:
				return -1;
			case ROOT:
				return 0;
			default:
				handleFailedMatch(getFileType());
			}
		default:
			handleFailedMatch(other.getFileType());
		}
		throw new IllegalStateException("This should not have happend!");
	}

	private static void handleFailedMatch(OmeroRemoteFileType datatype) {
		throw new IllegalStateException("The datatype " + datatype.toString() + "is not supported");
	}

}
