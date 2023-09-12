
package com.atakmap.android.dump1090.plugin;

import android.content.Context;
import com.atak.plugins.impl.AbstractPluginTool;
import gov.tak.api.util.Disposable;

public class dump1090Tool extends AbstractPluginTool implements Disposable {

    public dump1090Tool(Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher),
                "com.atakmap.android.dump1090.SHOW_PLUGIN");
        PluginNativeLoader.init(context);
    }

    @Override
    public void dispose() {
    }

}