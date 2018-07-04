package org.knime.knip.omero2.remote;

public enum OmeroRemoteFileType {
	DATASET("dataset"), IMAGE("image"), PROJECT("project"), ROOT("root");

	final String name;

	OmeroRemoteFileType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}