package com.gracecode.android.btgps;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import com.gracecode.android.btgps.helper.BroadcastHelper;
import com.gracecode.android.btgps.serivce.ConnectService;

import java.util.Set;

/**
 * Bluetooth GPS Application
 *
 * @author mingcheng
 * @since 13-11-22
 */
public class BluetoothGPS extends Application {

    public static final String ACTION_UPDATE_LOCATION = "bluetoothgps.action.updatelocation";
    public static final String ACTION_DEVICE_CONNECT_FAILED = "bluetoothgps.action.connect.failed";
    public static final String ACTION_DEVICE_CONNECT_SUCCESS = "bluetoothgps.action.connect.success";
    public static final String ACTION_DEVICE_DISCONNECTED = "bluetoothgps.action.disconnected";
    public static final String ACTION_DEVICE_CONNECTED = "bluetoothgps.action.connected";
    public static final String ACTION_PROVIDER_ADD = "bluetoothgps.action.provider.add";
    public static final String ACTION_PROVIDER_REMOVE = "bluetoothgps.action.provider.remove";
    public static final String ACTION_UPDATE_SATELLITE = "bluetoothgps.action.updatesatellite";
    public static final String ACTION_UPDATE_SENTENCE = "bluetoothgps.action.updatesentence";

    public static final String PREF_START_GPS = "startGps";
    public static final String PREF_GPS_LOCATION_PROVIDER = "gpsLocationProviderKey";
    public static final String PREF_REPLACE_STD_GPS = "replaceStdtGps";
    public static final String PREF_FORCE_ENABLE_PROVIDER = "forceEnableProvider";
    public static final String PREF_MOCK_GPS_NAME = "mockGpsName";
    public static final String PREF_CONNECTION_RETRIES = "connectionRetries";
    public static final String PREF_TRACK_RECORDING = "trackRecording";
    public static final String PREF_TRACK_FILE_DIR = "trackFileDirectory";
    public static final String PREF_TRACK_FILE_PREFIX = "trackFilePrefix";
    public static final String PREF_BLUETOOTH_DEVICE = "bluetoothDevice";
    public static final String PREF_ABOUT = "about";

    public static final String PREF_SIRF_GPS = "sirfGps";
    public static final String PREF_SIRF_ENABLE_GGA = "enableGGA";
    public static final String PREF_SIRF_ENABLE_RMC = "enableRMC";
    public static final String PREF_SIRF_ENABLE_GLL = "enableGLL";
    public static final String PREF_SIRF_ENABLE_VTG = "enableVTG";
    public static final String PREF_SIRF_ENABLE_GSA = "enableGSA";
    public static final String PREF_SIRF_ENABLE_GSV = "enableGSV";
    public static final String PREF_SIRF_ENABLE_ZDA = "enableZDA";
    public static final String PREF_SIRF_ENABLE_SBAS = "enableSBAS";
    public static final String PREF_SIRF_ENABLE_NMEA = "enableNMEA";
    public static final String PREF_SIRF_ENABLE_STATIC_NAVIGATION = "enableStaticNavigation";

    private static final String[] ACTION_ALL = new String[]{
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            ConnectService.ACTION_CONNECT,
            ConnectService.ACTION_DISCONNECT,
            ACTION_DEVICE_DISCONNECTED, ACTION_DEVICE_CONNECTED,
            ACTION_PROVIDER_ADD, ACTION_PROVIDER_REMOVE,
            ACTION_DEVICE_CONNECT_SUCCESS, ACTION_DEVICE_CONNECT_FAILED,
            ACTION_UPDATE_LOCATION, ACTION_UPDATE_SATELLITE, ACTION_UPDATE_SENTENCE
    };

    public static final String EXTRA_LOCATION = "bluetoothgps.extra.location";
    public static final String EXTRA_DEVICE = BluetoothDevice.EXTRA_NAME;
    public static final String EXTRA_PROVIDER = "bluetoothgps.extra.location.provider";
    public static final String EXTRA_SENTENCE = "bluetoothgps.extra.sentence";


    private BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onCreate() {
        super.onCreate();

        // Start device connect service.
        startService(BroadcastHelper.getConnectServerIntent(BluetoothGPS.this));

        // Start provider service.
//        startService();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    public boolean isSupported() {
        return (mBluetoothAdapter != null);
    }


    public boolean isEnabled() {
        if (isSupported()) {
            return mBluetoothAdapter.isEnabled();
        }

        return false;
    }


    public Set<BluetoothDevice> getPairedDevices() {
        if (isSupported()) {
            return mBluetoothAdapter.getBondedDevices();
        }

        return null;
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


    /**
     * Detect whether GPS Hardware is supported
     *
     * @return if supported return true
     */
    protected boolean isGPSSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }


    public BluetoothDevice getRemoteDevice(String address) {
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            return mBluetoothAdapter.getRemoteDevice(address);
        }

        return null;
    }

    public void connect(BluetoothDevice device) {
        Intent intent = new Intent(ConnectService.ACTION_CONNECT);
        intent.putExtra(EXTRA_DEVICE, device);
        sendBroadcast(intent);
    }

    public void disconnect() {
        sendBroadcast(new Intent(ConnectService.ACTION_DISCONNECT));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        disconnect();
    }
}
