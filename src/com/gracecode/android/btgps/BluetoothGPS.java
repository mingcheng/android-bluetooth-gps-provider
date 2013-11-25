package com.gracecode.android.btgps;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.gracecode.android.btgps.helper.BroadcastHelper;
import com.gracecode.android.btgps.serivce.ConnectService;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * User: mingcheng
 * Date: 13-11-22
 */
public class BluetoothGPS extends Application {
    public static final String UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String GPS_PROVIDER = "btgps";

    public static final String ACTION_UPDATE_LOCATION = "bluetoothgps.action.updatelocation";
    public static final String ACTION_DEVICE_CONNECT_FAILED = "bluetoothgps.action.connect.failed";
    public static final String ACTION_DEVICE_CONNECT_SUCCESS = "bluetoothgps.action.connect.success";
    public static final String ACTION_DEVICE_DISCONNECTED = "bluetoothgps.action.disconnected";
    public static final String ACTION_DEVICE_CONNECTED = "bluetoothgps.action.connected";
    public static final String ACTION_PROVIDER_ADD = "bluetoothgps.action.provider.add";
    public static final String ACTION_PROVIDER_REMOVE = "bluetoothgps.action.provider.remove";
    public static final String ACTION_UPDATE_SATELLITE = "bluetoothgps.action.updatesatellite";

    private static final String[] ACTION_ALL = new String[]{
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            ConnectService.ACTION_CONNECT,
            ConnectService.ACTION_DISCONNECT,
            ACTION_DEVICE_DISCONNECTED, ACTION_DEVICE_CONNECTED,
            ACTION_PROVIDER_ADD, ACTION_PROVIDER_REMOVE,
            ACTION_DEVICE_CONNECT_SUCCESS, ACTION_DEVICE_CONNECT_FAILED,
            ACTION_UPDATE_LOCATION, ACTION_UPDATE_SATELLITE
    };

    public static final String EXTRA_LOCATION = "bluetoothgps.extra.location";
    public static final String EXTRA_DEVICE = BluetoothDevice.EXTRA_NAME;
    public static final String EXTRA_PROVIDER = "bluetoothgps.extra.location.provider";

    private static BluetoothGPS mInstance;

    public static BluetoothGPS getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = BluetoothGPS.this;

        // Start device connect service.
        startService(BroadcastHelper.getConnectServerIntent(BluetoothGPS.this));

        // Start provider service.
        startService(BroadcastHelper.getProviderServerIntent(BluetoothGPS.this, GPS_PROVIDER));
    }

    public static UUID getUUID() {
        return UUID.fromString(BluetoothGPS.UUID_STRING);
    }

    public SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(BluetoothGPS.this);
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        for (int i = 0; i < ACTION_ALL.length; i++) {
            intentFilter.addAction(ACTION_ALL[i]);
        }

        return intentFilter;
    }
}
