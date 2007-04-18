package org.csstudio.sds.components.epics;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * 
 * @author Sven Wende
 * 
 */
public final class Activator extends Plugin {

	/**
	 *  The plug-in ID.
	 */
	public static final String PLUGIN_ID = "org.csstudio.sds.components.epics"; //$NON-NLS-1$

	/**
	 * The shared instance.
	 */
	private static Activator _plugin;
	
	/**
	 * The constructor.
	 */
	public Activator() {
		_plugin = this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		_plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return _plugin;
	}

}
