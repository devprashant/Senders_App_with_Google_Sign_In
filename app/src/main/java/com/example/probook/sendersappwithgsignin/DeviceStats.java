package com.example.probook.sendersappwithgsignin;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.telecom.TelecomManager;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

/**
 * Created by probook on 6/4/2016.
 */
public class DeviceStats{

    private String bttLevel;
    private String chargeMethod;
    private String chargeState;
    private String bttTemp;
    private String networkType;
    private String ntwOperator;

    private Context context;

    public DeviceStats(Context context) {
        this.context = context;

        // battery related
        this.bttLevel = setBttLevel();
        this.chargeMethod = setChargeMethod();
        this.chargeState  = setChargeState();
        this.bttTemp = setBttTemp();
        // network related
        this.networkType = setNetworkType();
        this.ntwOperator = setNtwOperator();
    }

    public String getBttTemp() {
        return bttTemp;
    }

    private String setBttTemp() {
        Intent bttIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float temp = bttIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10;
        return String.valueOf(temp);
    }

    public String getNtwOperator() {
        return ntwOperator;
    }

    private String setNtwOperator() {
        final TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkOperatorName();
    }

    public String getNetworkType() {
        return networkType;
    }

    private String setNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if(networkInfo != null && (networkInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS
                || networkInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE)){
            return "2G";
        } else {
            return "3G";
        }
    }


    public String getBttLevel() {
        return bttLevel;
    }

    private String setBttLevel() {
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawlevel = batteryIntent.getIntExtra("level", -1);
        double scale = batteryIntent.getIntExtra("scale", -1);
        double level = -1;
        if (rawlevel >=0 && scale > 0){
            level = rawlevel / scale;
        }

        return String.valueOf(level);
    }

    public String getChargeState() {
        return chargeState;
    }

    private String setChargeState() {
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        // Are we charging / charged ?
        boolean chargingState = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        return String.valueOf(chargingState);
    }

    public String getChargeMethod() {
        return chargeMethod;
    }

    private  String setChargeMethod() {
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // How are we charging ?
        int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        switch (chargePlug){
            case BatteryManager.BATTERY_PLUGGED_USB: return "usb";

            case BatteryManager.BATTERY_PLUGGED_AC: return  "ac";

            case BatteryManager.BATTERY_PLUGGED_WIRELESS: return  "wireless";

            default: return "unknown";
        }
    }

    private void test(){
        
    }
}
