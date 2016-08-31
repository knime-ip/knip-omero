package org.knime.knip.newomero.util;

import org.knime.scijava.core.ResourceAwareClassLoader;
import org.knime.scijava.core.pluginindex.ReusablePluginIndex;
import org.scijava.Context;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;

/**
 * Gateway to the SciJava world
 */
public class OmeroGateway {

	/** singleton instance */
	protected static OmeroGateway instance = null;

	/** the gateways class loader */
	protected ResourceAwareClassLoader classLoader = null;

	/**
	 * the cached plugin index. Building the plugin index only needs to be done
	 * once.
	 */
	protected PluginIndex pluginIndex = null;

	/**
	 * The global context for all KNIP-2.0 Nodes.
	 */
	private Context globalContext;

	/**
	 * Constructor. Only to be called from {@link #get()}.
	 */
	private OmeroGateway() {
		classLoader = new ResourceAwareClassLoader(getClass().getClassLoader(), getClass());

		pluginIndex = new ReusablePluginIndex(new DefaultPluginFinder(classLoader));
	}

	/**
	 * Get the Gateway instance.
	 *
	 * @return the singletons instance
	 */
	public static synchronized OmeroGateway get() {
		if (instance == null) {
			instance = new OmeroGateway();
		}
		return instance;
	}

	public Context getGlobalContext() {
		if (globalContext == null) {
			globalContext = new Context(pluginIndex);

		}
		return globalContext;
	}

	/**
	 * Get the {@link ResourceAwareClassLoader} used by this Gateways contexts.
	 *
	 * @return class loader for the contexts
	 */
	public ResourceAwareClassLoader getClassLoader() {
		return classLoader;
	}

}
