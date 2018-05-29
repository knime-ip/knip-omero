package org.knime.knip.nio;

import io.scif.SCIFIO;

import net.imagej.ops.OpService;

import org.knime.knip.core.KNIPGateway;
import org.knime.scijava.core.ResourceAwareClassLoader;
import org.scijava.Context;
import org.scijava.io.handle.DataHandleService;
import org.scijava.io.location.LocationService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.service.Service;

public class NIOGateway {

	private static NIOGateway m_instance;

	private static DataHandleService m_handles;
	private static LocationService m_loc;
	private static Context m_context;

	private static SCIFIO m_scifio;

	private NIOGateway() {
		m_context = new Context(new PluginIndex(
				new DefaultPluginFinder(new ResourceAwareClassLoader(getClass().getClassLoader(), getClass()))));
	}

	/**
	 * @return singleton instance of {@link KNIPGateway}
	 */
	public static synchronized NIOGateway getInstance() {
		if (m_instance == null) {
			m_instance = new NIOGateway();
		}
		return m_instance;
	}

	/**
	 * @return singleton instance of {@link DataHandleService}
	 */
	public static DataHandleService handles() {
		if (m_handles == null) {
			m_handles = getInstance().m_context.getService(DataHandleService.class);
		}
		return m_handles;
	}

	public static LocationService locations() {
		if (m_loc == null) {
			m_loc = getInstance().m_context.getService(LocationService.class);
		}
		return m_loc;
	}

	public static SCIFIO scifio() {
		if (m_scifio == null) {
			m_scifio = new SCIFIO(getInstance().m_context);
		}
		return m_scifio;
	}

	public static <S extends Service> S getService(Class<S> c) {
		return getInstance().m_context.getService(c);
	}

	public static Context context() {
		return getInstance().m_context;
	}

	public static OpService ops() {
		return getInstance().m_context.getService(OpService.class);
	}
}
