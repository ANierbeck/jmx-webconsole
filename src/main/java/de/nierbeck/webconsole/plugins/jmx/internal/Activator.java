package de.nierbeck.webconsole.plugins.jmx.internal;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
	
    /** Registration for the plugin. */
    private ServiceRegistration pluginRegistration;
	private JmxPluginServlet jmxPlugin;

	
	public void start(BundleContext bundleContext) throws Exception {
		jmxPlugin = new JmxPluginServlet();
		Properties props = new Properties();
		props.put( "felix.webconsole.label", JmxPluginConstants.LABEL);
		props.put( "felix.webconsole.title", JmxPluginConstants.NAME);
		props.put( "felix.webconsole.css", "/jmx/res/ui/jmx.css");
		props.put( Constants.SERVICE_DESCRIPTION, "JMX Plugin for the Apache Felix Web Console" );
		props.put( Constants.SERVICE_VENDOR, "www.nierbeck.de" );
		this.pluginRegistration = bundleContext.registerService(javax.servlet.Servlet.class.getName(),
				jmxPlugin, props);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if ( this.pluginRegistration != null )
        {
            this.pluginRegistration.unregister();
            this.pluginRegistration = null;
        }
        if ( this.jmxPlugin != null ) {
            this.jmxPlugin.destroy();
            this.jmxPlugin = null;
        }
	}
}
