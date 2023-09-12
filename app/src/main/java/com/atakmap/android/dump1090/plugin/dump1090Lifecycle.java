
package com.atakmap.android.dump1090.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;

import com.atakmap.android.dump1090.dump1090MapComponent;

public class dump1090Lifecycle extends AbstractPlugin implements IPlugin {

    public dump1090Lifecycle(IServiceController serviceController) {
        super(serviceController, new dump1090Tool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new dump1090MapComponent());
    }
}