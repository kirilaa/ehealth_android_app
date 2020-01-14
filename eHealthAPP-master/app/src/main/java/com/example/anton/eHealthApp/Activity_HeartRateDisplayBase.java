/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
 */

package com.example.anton.eHealthApp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.DataState;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.ICalculatedRrIntervalReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IPage4AddtDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.RrFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IRssiReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.ICumulativeOperatingTimeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IManufacturerAndSerialReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusLegacyCommonPcc.IVersionAndModelReceiver;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.juang.jplot.PlotPlanitoXY;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;


/**
 * Base class to connects to Heart Rate Plugin and display all the event data.
 */
public abstract class Activity_HeartRateDisplayBase extends AppCompatActivity {
    protected abstract void requestAccessToPcc();

    int location_samples_average = 10;
    int location_count_samples = 0;
    double latitude_sum = 0.0;
    double longitude_sum = 0.0;
    LocationCallback locationCallback;
    LocationRequest locationRequest;
    PlotPlanitoXY plot;
    LinearLayout grafica;
    TextView tv_computedHeartRate;
    TextView locationView;
    TextView titleHeartRate;
    long heartBeatCounter;
    private boolean conexion;
    private boolean dialog = false;
    Intent intent;
    boolean noSigas = false;
    Context context;
    boolean zerosent = false;

    float[] x = new float[100];
    float[] y = new float[100];

    AntPlusHeartRatePcc hrPcc = null;
    protected PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;
    protected PostJSON postJSON = new PostJSON();

    int latitude;
    int longitude;

    @Override
    protected void onResume() {
        super.onResume();

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3 * 1000);
        Looper l = Looper.myLooper();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {

//                        latitude =(int) Math.round(location.getLatitude() * 1000000);
//                        longitude = (int) Math.round(location.getLongitude() * 1000000);
//                        locationView.setText("lat:"+latitude+", long:"+longitude);
//                        Log.d("location", "lat:"+latitude+", long:"+longitude);

                        if(location_count_samples == (location_samples_average-1)) {
                            latitude =(int) Math.round((latitude_sum/location_count_samples) * 1000000);
                            longitude = (int) Math.round((longitude_sum/location_count_samples) * 1000000);
                            Log.d("in_loc_if", "lat:"+latitude+", long:"+longitude);
                            locationView.setText("lat:"+latitude+", long:"+longitude);
                            location_count_samples = 0;
                            latitude_sum = 0.0;
                            longitude_sum = 0.0;
                        }
                        else {
                            //latitude_sum += Math.round(location.getLatitude() * 1000000);
                            //longitude_sum += Math.round(location.getLongitude() * 1000000);
                            latitude_sum += location.getLatitude();
                            longitude_sum += location.getLongitude();
//                            Log.d("location", "lat:"+latitude_sum+", long:"+longitude_sum);
                            location_count_samples++;
                        }


                    }
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, l);

        context = this;

        for (int j = 0; j < 100; j++) {
            x[j] = j;
            y[j] = 0;
        }

        showDataDisplay();
//        subscribeToHrEvents();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        handleReset();


//        fusedLocationClient.getLastLocation()
//                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
//                    @Override
//                    public void onSuccess(Location location) {
//                        // Got last known location. In some rare situations this can be null.
//                        if (location != null) {
//                            latitude = (int) Math.round(location.getLatitude() * 1000000);
//                            longitude = (int) Math.round(location.getLongitude() * 1000000);
//                        }
//                    }
//                });


    }


    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    protected void handleReset()
    {
        //Release the old access if it exists
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }

        requestAccessToPcc();
    }

    protected void showDataDisplay()
    {
        setContentView(R.layout.activity_heart_rate);
        tv_computedHeartRate = findViewById(R.id.heartRate);
        grafica= findViewById(R.id.grafica);
        locationView = findViewById(R.id.location_view);

        titleHeartRate = findViewById(R.id.heart_rate_title);

        titleHeartRate.setVisibility(View.INVISIBLE);
        //Comment to show the real-time location of the patient
//        locationView.setVisibility(View.INVISIBLE);
        //Reset the text display
        tv_computedHeartRate.setText("---");


    }

    /**
     * Switches the active view to the data display and subscribes to all the data events
     */
    public void subscribeToHrEvents()
    {
        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver()
        {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                final int computedHeartRate, final long heartBeatCount,
                final BigDecimal heartBeatEventTime, final DataState dataState)
            {

                // Mark heart rate with asterisk if zero detected
                final String textHeartRate = ((DataState.ZERO_DETECTED.equals(dataState)) ? "0" : String.valueOf(computedHeartRate));

                // Mark heart beat count and heart beat event time with asterisk if initial value
                /*final String textHeartBeatCount = String.valueOf(heartBeatCount)
                    + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");
                final String textHeartBeatEventTime = String.valueOf(heartBeatEventTime)
                    + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");*/
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tv_computedHeartRate.setText(textHeartRate);

                        grafica.removeAllViews();
                        System.arraycopy(y, 1, y, 0, 99);
                        y[99] = computedHeartRate;//datos
                        plot = new PlotPlanitoXY(context, "", "x", "valor pulsación");
                        plot.SetSerie1(x, y, "PPM", 0, true);
                        plot.SetColorSerie1(255,76,76);
                        plot.SetColorFondo(0, 255,255,255);
                        plot.SetColorCuadricula(255,255,255);
                        plot.SetSizeLeyend(1);
                        plot.SetGruesoLinea(10);
//                                plot.SetColorTitulo (0,0,0);
//                                plot.SetSizeTituloX (20);
                        plot.SetColorTituloX (255,255,255);
                        plot.SetHD(true);
                        plot.SetTouch(false);
                        plot.SetEscalaY1(50, 200);
                        grafica.addView(plot);

//                        if(((heartBeatCount != heartBeatCounter && !noSigas) || (DataState.ZERO_DETECTED.equals(dataState)&& !zerosent)) &&
//                                !(heartBeatCount == 1 && (DataState.ZERO_DETECTED.equals(dataState))) ){
//                            heartBeatCounter = heartBeatCount;
                            try {
                                Date date = Calendar.getInstance().getTime();
                                long ms = date.getTime();
                                if(DataState.ZERO_DETECTED.equals(dataState) && !(postJSON.isTimerActive())) {
                                    conexion = postJSON.startRequestEmergency(0, ms, latitude, longitude);
                                    Log.d("connection", "zero triggered");
                                    //zerosent=true;
                                }
                                else if(!postJSON.isTimerActive()){
                                    conexion = postJSON.startRequestEmergency(computedHeartRate, ms, latitude, longitude);
                                    Log.d("connection", computedHeartRate + "active");
                                }
//                                if (postJSON.isTimerActive()) {
//                                   noSigas = true;
//                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if(!conexion && intent == null && !dialog) {
                                dialog = true;
                                System.out.println("ERROR");
                                 AlertDialog.Builder builder =
                                            new AlertDialog.Builder(Activity_HeartRateDisplayBase.this);
                                 builder.setMessage("No se ha podido establecer conexión con el servidor")
                                         .setTitle("CONECTION ERROR")
                                         .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                             public void onClick(DialogInterface dialog, int id) {
                                                 dialog.cancel();
                                                 intent = new Intent(Activity_HeartRateDisplayBase.this, Intro.class);
                                                 startActivity(intent);
                                                 finish();
                                             }
                                         });
                                 AlertDialog alert = builder.create();
                                 alert.show();
                            }
                            if(conexion && intent == null && postJSON.isTimerActive()){
                                Log.d("intent", "start");
                                zerosent=false;
                                intent = new Intent(Activity_HeartRateDisplayBase.this, Pregunta.class);
                                intent.putExtra("pid", postJSON.getPid());
                                intent.putExtra("latitude", latitude);
                                intent.putExtra("longitude", longitude);
//                                postJSON.reset();
                                startActivity(intent);

//                                finish();
                            }



//                        }


                    }
                });
            }
        });

        hrPcc.subscribePage4AddtDataEvent(new IPage4AddtDataReceiver()
        {
            @Override
            public void onNewPage4AddtData(final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                final int manufacturerSpecificByte,
                final BigDecimal previousHeartBeatEventTime)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {

                        //tv_manufacturerSpecificByte.setText(String.format("0x%02X", manufacturerSpecificByte));
                        //tv_previousHeartBeatEventTime.setText(String.valueOf(previousHeartBeatEventTime));

                    }
                });
            }
        });

        hrPcc.subscribeCumulativeOperatingTimeEvent(new ICumulativeOperatingTimeReceiver()
        {
            @Override
            public void onNewCumulativeOperatingTime(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final long cumulativeOperatingTime)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                       // tv_estTimestamp.setText(String.valueOf(estTimestamp));

                        //tv_cumulativeOperatingTime.setText(String.valueOf(cumulativeOperatingTime));
                    }
                });
            }
        });

        hrPcc.subscribeManufacturerAndSerialEvent(new IManufacturerAndSerialReceiver()
        {
            @Override
            public void onNewManufacturerAndSerial(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int manufacturerID,
                final int serialNumber)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                       // tv_estTimestamp.setText(String.valueOf(estTimestamp));

                       // tv_manufacturerID.setText(String.valueOf(manufacturerID));
                       // tv_serialNumber.setText(String.valueOf(serialNumber));
                    }
                });
            }
        });

        hrPcc.subscribeVersionAndModelEvent(new IVersionAndModelReceiver()
        {
            @Override
            public void onNewVersionAndModel(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int hardwareVersion,
                final int softwareVersion, final int modelNumber)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                      //  tv_estTimestamp.setText(String.valueOf(estTimestamp));

                      //  tv_hardwareVersion.setText(String.valueOf(hardwareVersion));
                      //  tv_softwareVersion.setText(String.valueOf(softwareVersion));
                      //  tv_modelNumber.setText(String.valueOf(modelNumber));
                    }
                });
            }
        });

        hrPcc.subscribeCalculatedRrIntervalEvent(new ICalculatedRrIntervalReceiver()
        {
            @Override
            public void onNewCalculatedRrInterval(final long estTimestamp,
                EnumSet<EventFlag> eventFlags, final BigDecimal rrInterval, final RrFlag flag)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                       // tv_estTimestamp.setText(String.valueOf(estTimestamp));
                       // tv_rrFlag.setText(flag.toString());

                        // Mark RR with asterisk if source is not cached or page 4
                       // if (flag.equals(RrFlag.DATA_SOURCE_CACHED)
                        //    || flag.equals(RrFlag.DATA_SOURCE_PAGE_4))
                         //   tv_calculatedRrInterval.setText(String.valueOf(rrInterval));
                       // else
                         //   tv_calculatedRrInterval.setText(String.valueOf(rrInterval) + "*");
                    }
                });
            }
        });

        hrPcc.subscribeRssiEvent(new IRssiReceiver() {
            @Override
            public void onRssiData(final long estTimestamp, final EnumSet<EventFlag> evtFlags, final int rssi) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      //  tv_estTimestamp.setText(String.valueOf(estTimestamp));
                       // tv_rssi.setText(String.valueOf(rssi) + " dBm");
                    }
                });
            }
        });
    }

    protected IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
        new IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
        {
        //Handle the result, connecting to events on success or reporting failure to user.
        @Override
        public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
            DeviceState initialDeviceState)
        {
//            showDataDisplay();
            switch(resultCode)
            {
                case SUCCESS:
                    hrPcc = result;
                    //tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);
                    subscribeToHrEvents();

                  //  if(!result.supportsRssi()) tv_rssi.setText("N/A");
                    break;
                case CHANNEL_NOT_AVAILABLE:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                   // tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case ADAPTER_NOT_DETECTED:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                   // tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case BAD_PARAMS:
                    //Note: Since we compose all the params ourself, we should never see this result
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                   // tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case OTHER_FAILURE:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                   // tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                   // tv_status.setText("Error. Do Menu->Reset.");
                    AlertDialog.Builder adlgBldr = new AlertDialog.Builder(Activity_HeartRateDisplayBase.this);
                    adlgBldr.setTitle("Missing Dependency");
                    adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() +
                            "\"\n was not found. You need to install the ANT+ Plugins service or you " +
                            "may need to update your existing version if you already have it. Do you want" +
                            " to launch the Play Store to get it?");
                    adlgBldr.setCancelable(true);
                    adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent startStore;
                            startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                            startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            Activity_HeartRateDisplayBase.this.startActivity(startStore);
                        }
                    });
                    adlgBldr.setNegativeButton("Cancel", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    final AlertDialog waitDialog = adlgBldr.create();
                    waitDialog.show();
                    break;
                case USER_CANCELLED:
                  //  tv_status.setText("Cancelled. Do Menu->Reset.");
                    break;
                case UNRECOGNIZED:
                    Toast.makeText(Activity_HeartRateDisplayBase.this,
                        "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                        Toast.LENGTH_SHORT).show();
                 //   tv_status.setText("Error. Do Menu->Reset.");
                    break;
                default:
                    Toast.makeText(Activity_HeartRateDisplayBase.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                 //   tv_status.setText("Error. Do Menu->Reset.");
                    break;
            }
        }
        };

        //Receives state changes and shows it on the status display line
        protected  IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new IDeviceStateChangeReceiver()
        {
            @Override
            public void onDeviceStateChange(final DeviceState newDeviceState)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                  //      tv_status.setText(hrPcc.getDeviceName() + ": " + newDeviceState);
                    }
                });


            }
        };

        @Override
        protected void onDestroy()
        {
            if(releaseHandle != null)
            {
                releaseHandle.close();
            }
            super.onDestroy();
        }
}
