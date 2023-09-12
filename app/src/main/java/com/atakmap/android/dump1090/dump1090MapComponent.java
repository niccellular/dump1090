
package com.atakmap.android.dump1090;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.dump1090.plugin.R;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public class dump1090MapComponent extends DropDownMapComponent {

    private static final String TAG = "dump1090MapComponent";

    private Context pluginContext;

    private dump1090DropDownReceiver ddr;

    private Socket dump1090socket;
    private InputStream inputStream;
    private Thread thread;
    private Runnable runnable;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        ddr = new dump1090DropDownReceiver(
                view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(dump1090DropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

        runnable = () -> {
            while (true) {
                try {
                    int port = 30003;
                    InetAddress inetAddress = InetAddress.getByName("localhost");
                    SocketAddress socketAddress = new InetSocketAddress(inetAddress, port);
                    dump1090socket = new Socket();
                    //dump1090socket.bind(socketAddress);
                    while(!dump1090socket.isConnected()) {
                        dump1090socket.connect(socketAddress, 0);
                        Thread.sleep(1000);
                    }
                    inputStream = dump1090socket.getInputStream();
                    Log.i(TAG, "Socket setup complete");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (dump1090socket.isConnected()) {
                    if (!ddr.isEnabled)
                        continue;
                    try {
                        if (inputStream.available() > 0) {
                            try {
                                byte[] bytes = new byte[inputStream.available()];
                                int count = inputStream.read(bytes);
                                if (count > 0) {
                                    String s = new String(bytes);
                                    String[] stringList = s.split("\\r?\\n");
                                    for (String entry : stringList) {
                                        Log.d(TAG, entry);
                                        String[] e = entry.split(",");

                                        if (e[4].isEmpty() || e[14].isEmpty() || e[15].isEmpty()) {
                                            Log.i(TAG, "ICAO or GPS was missing");
                                            continue;
                                        } else {
                                            //MSG,8,1,1,ABD3C7,1,2023/01/07,20:43:54.849,2023/01/07,20:43:54.849,AAL463,19525,414,162,28.087805,-80.629578,2560,0722,0,0,0,0

                                            double lat = 0, lng = 0, altitude = 0, speed = 0, track = 0;
                                            if (!e[14].isEmpty())
                                                lat = Double.parseDouble(e[14]);
                                            if (!e[15].isEmpty())
                                                lng = Double.parseDouble(e[15]);
                                            if (!e[11].isEmpty())
                                                altitude = Double.parseDouble(e[11]);
                                            if (!e[12].isEmpty())
                                                speed = Double.parseDouble(e[12]);
                                            if (!e[13].isEmpty())
                                                track = Double.parseDouble(e[13]);

                                            if (!ddr.altitudeFilter.isEmpty() && altitude < Double.parseDouble(ddr.altitudeFilter)) {
                                                Log.i(TAG, "Altitude limit");
                                                continue;
                                            }

                                            if (ddr.icaoFilter.length > 0 && !Arrays.asList(ddr.icaoFilter).contains(e[4])) {
                                                Log.i(TAG, "ICAO not in filter");
                                                continue;
                                            }

                                            CotEvent cotEvent = new CotEvent();

                                            CoordinatedTime time = new CoordinatedTime();
                                            cotEvent.setTime(time);
                                            cotEvent.setStart(time);
                                            cotEvent.setStale(time.addMinutes(1));

                                            cotEvent.setUID(e[4]);

                                            // civilian
                                            if (!e[4].isEmpty() && (Integer.parseInt(e[4], 16) < 0xADF7C8) || (Integer.parseInt(e[4], 16) > 0xAFFFFF)) {
                                                //if (typeDesc.startsWith("H"))
                                                // rotary
                                                //    cotEvent.setType("a-f-A-C-H");
                                                //else
                                                // fixed wing
                                                cotEvent.setType("a-f-A-C-F");
                                                // military
                                            } else {
                                                //if (typeDesc.startsWith("H"))
                                                // rotary
                                                //    cotEvent.setType("a-f-A-M-H");
                                                //else
                                                // fixed wing
                                                cotEvent.setType("a-f-A-M-F");
                                            }

                                            cotEvent.setHow("m-g");

                                            // compute HAE from adsb data
                                            double hae = 0;
                                            if (lat > 0 && lng > 0)
                                                hae = EGM96.getHAE(lat, lng, altitude);
                                            //Log.i(TAG, String.format("HAE: %f ALT: %f", hae, altitude));
                                            //cotPoint = new CotPoint(lat, lng, hae, 10, 2);
                                            CotPoint cotPoint = new CotPoint(lat, lng, altitude, hae, 0);
                                            cotEvent.setPoint(cotPoint);

                                            CotDetail cotDetail = new CotDetail("detail");
                                            cotEvent.setDetail(cotDetail);

                                            CotDetail cotRemark = new CotDetail("remarks");
                                            cotRemark.setAttribute("source", "ADSB Position");
                                            cotRemark.setInnerText(String.format(Locale.US, "Raw data: %s", entry));

                                            CotDetail cotTrack = new CotDetail("track");
                                            cotTrack.setAttribute("speed", String.valueOf(speed));
                                            cotTrack.setAttribute("course", String.valueOf(track));

                                            // title is ICAO or Callsign if present
                                            String name = e[4];
                                            if (!e[10].isEmpty()) {
                                                Log.i(TAG, "Callsign is not empty");
                                                name = e[10];
                                            }

                                            CotDetail cotContact = new CotDetail("contact");
                                            cotContact.setAttribute("callsign", String.format(Locale.US, "%s - %s ft", name, e[11]));

                                            cotDetail.addChild(cotRemark);
                                            cotDetail.addChild(cotContact);
                                            cotDetail.addChild(cotTrack);

                                            if (cotEvent.isValid()) {
                                                CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
                                                if (ddr.exportEnabled) {
                                                    CotMapComponent.getExternalDispatcher().dispatch(cotEvent);
                                                }
                                                Log.i(TAG, "Created CoT");
                                            } else {
                                                Log.i(TAG, "Invalid CoT");
                                            }
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Thread.sleep(1000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
       thread = new Thread(runnable);
       thread.start();
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }

}
