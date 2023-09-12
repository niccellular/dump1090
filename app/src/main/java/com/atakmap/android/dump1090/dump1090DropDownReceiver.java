
package com.atakmap.android.dump1090;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;


import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dump1090.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.log.Log;

public class dump1090DropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = "dump1090DropDownReceiver";

    public static final String SHOW_PLUGIN = "com.atakmap.android.dump1090.SHOW_PLUGIN";
    private final View templateView;
    private final Context pluginContext;
    private Button enableBtn;
    public boolean isEnabled = false;
    private EditText icaoList, altitudeLimit;
    private Switch exportBtn;
    public boolean exportEnabled = false;
    public String[] icaoFilter = new String[0];
    public String altitudeFilter = "";


    /**************************** CONSTRUCTOR *****************************/

    public dump1090DropDownReceiver(final MapView mapView,
            final Context context) {
        super(mapView);
        this.pluginContext = context;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        templateView = PluginLayoutInflater.inflate(context,
                R.layout.main_layout, null);

        enableBtn = templateView.findViewById(R.id.enableBtn);
        icaoList = templateView.findViewById(R.id.IcaoList);
        altitudeLimit = templateView.findViewById(R.id.AltitudeLimit);
        exportBtn = templateView.findViewById(R.id.exportBtn);

    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {

            Log.d(TAG, "showing plugin drop down");
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);

            enableBtn.setOnClickListener(view -> {
                if ( ((String)enableBtn.getText()).contains("OFF") ){
                    Log.i(TAG, "Turning receiver on");
                    enableBtn.setBackgroundResource(R.color.green);
                    enableBtn.setText("Receiving *ON*");
                    isEnabled = true;
                } else {
                    Log.i(TAG, "Turning receiver off");
                    enableBtn.setBackgroundResource(R.color.red);
                    enableBtn.setText("Receiving *OFF*");
                    isEnabled = false;
                }
            });

            icaoList.setOnFocusChangeListener((view, focus) -> {
                if (!focus) {
                    Editable e = icaoList.getText();
                    icaoFilter = e.toString().split(",");
                }
            });

            altitudeLimit.setOnFocusChangeListener((view, focus) -> {
                if (!focus) {
                    Editable e = altitudeLimit.getText();
                    altitudeFilter = e.toString();
                }
            });

            exportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    exportEnabled = !exportEnabled;
                }
            });
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
