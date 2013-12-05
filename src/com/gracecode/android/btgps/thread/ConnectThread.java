package com.gracecode.android.btgps.thread;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.os.Build;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.task.ReadNmeaTask;
import com.gracecode.android.common.Logger;
import com.gracecode.android.btgps.util.SirfWraper;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    public static final String PREF_SIRF_GPS = BluetoothGPS.PREF_SIRF_GPS;
    public static final String PREF_SIRF_ENABLE_GGA = BluetoothGPS.PREF_SIRF_ENABLE_GGA;
    public static final String PREF_SIRF_ENABLE_RMC = BluetoothGPS.PREF_SIRF_ENABLE_RMC;
    public static final String PREF_SIRF_ENABLE_GLL = BluetoothGPS.PREF_SIRF_ENABLE_GLL;
    public static final String PREF_SIRF_ENABLE_VTG = BluetoothGPS.PREF_SIRF_ENABLE_VTG;
    public static final String PREF_SIRF_ENABLE_GSA = BluetoothGPS.PREF_SIRF_ENABLE_GSA;
    public static final String PREF_SIRF_ENABLE_GSV = BluetoothGPS.PREF_SIRF_ENABLE_GSV;
    public static final String PREF_SIRF_ENABLE_ZDA = BluetoothGPS.PREF_SIRF_ENABLE_ZDA;
    public static final String PREF_SIRF_ENABLE_SBAS = BluetoothGPS.PREF_SIRF_ENABLE_SBAS;
    public static final String PREF_SIRF_ENABLE_NMEA = BluetoothGPS.PREF_SIRF_ENABLE_NMEA;
    public static final String PREF_SIRF_ENABLE_STATIC_NAVIGATION = BluetoothGPS.PREF_SIRF_ENABLE_STATIC_NAVIGATION;

    private final Context mContext;
    private final OnStatusChangeListener mListener;
    private final BluetoothDevice mBluetoothDevice;
    private final SharedPreferences mSharedPreferences;
    private boolean isKeepRunning = true;
    public static final String DEFAULT_UUID = "00001101-0000-1000-8000-00805f9b34fb";
    private int mRetries = 0;
    private BluetoothSocket mBluetoothDeviceSocket;
    private ReadNmeaTask mReadNmeaTask;
    private SirfWraper mSirfWraper;


    public interface OnStatusChangeListener extends ReadNmeaTask.OnNmeaReadListener {
        abstract public void onConnected();

        abstract public void onConnectedFailed(int retries);

        abstract public void onDisConnected();

        abstract public void onStartConnect();
    }


    public ConnectThread(Context context, BluetoothDevice device, OnStatusChangeListener listener) {
        mContext = context;
        mBluetoothDevice = device;
        mListener = listener;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public int getMaxRetries() {
        String retries = mSharedPreferences.getString(
                BluetoothGPS.PREF_CONNECTION_RETRIES,
                mContext.getString(R.string.defaultConnectionRetries)
        );
        return Integer.parseInt(retries);
    }

    /**
     * Get device's uuid
     *
     * @param device device
     * @return uuid
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private UUID getUUID(BluetoothDevice device) {
        try {
            ParcelUuid[] uuids = device.getUuids();
            if (uuids.length > 0) {
                for (int i = 0; i < uuids.length; i++) {
                    return uuids[i].getUuid();
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return getUUID();
    }

    private UUID getUUID() {
        return UUID.fromString(DEFAULT_UUID);
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    mBluetoothDeviceSocket = null;
                    break;
            }
        }
    };


    @Override
    public void run() {
        mContext.registerReceiver(mBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        try {
            while (isKeepRunning) {
                if (!isConnected()) {
                    if (mRetries > getMaxRetries() - 1) {
                        disconnect();
                    } else {
                        connect();
                    }
                } else {
                    Logger.v("Device '" + mBluetoothDevice.getName() + "' is connected.");
                    mRetries = 0;
                }

                Thread.sleep(5000l);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }
    }

    public void connect() {
        try {
            UUID uuid = getUUID(mBluetoothDevice);
            mListener.onStartConnect();
            if (true) {
                mBluetoothDeviceSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            } else {
                mBluetoothDeviceSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            }

            // Start connect
            mBluetoothDeviceSocket.connect();

            if (mBluetoothDeviceSocket.isConnected()) {

                // Send Sirf Command
                mSirfWraper = new SirfWraper(mContext, mBluetoothDeviceSocket.getOutputStream());
                if (isSirfDevice()) {
                    Logger.v("Make as Sirf GPS, Send some commands.");
                    asSirfDevice();
                }

                // Read NMEA Sentence
                mReadNmeaTask = new ReadNmeaTask(mContext, mBluetoothDeviceSocket.getInputStream(), mListener);
                mReadNmeaTask.run();
                mListener.onConnected();
            } else {
                throw new IOException("Device doesn't connected.");
            }
        } catch (IOException e) {
            try {
                if (mReadNmeaTask != null) {
                    mReadNmeaTask.disconnect();
                }

                mBluetoothDeviceSocket = null;
            } finally {
                mListener.onConnectedFailed(++mRetries);
            }
        }
    }

    private boolean isSirfDevice() {
        return mSharedPreferences.contains(PREF_SIRF_GPS) && mSharedPreferences.getBoolean(PREF_SIRF_GPS, false);
    }

    private void asSirfDevice() {
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_GLL)) {
            mSirfWraper.enableNmeaGLL(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_GLL, false));
        }
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_VTG)) {
            mSirfWraper.enableNmeaVTG(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_VTG, false));
        }
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_GSA)) {
            mSirfWraper.enableNmeaGSA(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_GSA, false));
        }
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_GSV)) {
            mSirfWraper.enableNmeaGSV(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_GSV, false));
        }
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_ZDA)) {
            mSirfWraper.enableNmeaZDA(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_ZDA, false));
        }
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_STATIC_NAVIGATION)) {
            mSirfWraper.enableStaticNavigation(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_STATIC_NAVIGATION, false));
        } else if (mSharedPreferences.contains(PREF_SIRF_ENABLE_NMEA)) {
            mSirfWraper.enableNMEA(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_NMEA, true));
        }
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_SBAS)) {
            mSirfWraper.enableSBAS(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_SBAS, true));
        }
        mSirfWraper.sendNmeaCommand(getString(R.string.sirf_nmea_gga_on));
        mSirfWraper.sendNmeaCommand(getString(R.string.sirf_nmea_rmc_on));
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_GGA)) {
            mSirfWraper.enableNmeaGGA(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_GGA, true));
        }
        if (mSharedPreferences.contains(PREF_SIRF_ENABLE_RMC)) {
            mSirfWraper.enableNmeaRMC(mSharedPreferences.getBoolean(PREF_SIRF_ENABLE_RMC, true));
        }
    }

    private String getString(int resId) {
        return mContext.getString(resId);
    }

    private void disconnect(boolean notify) {
        try {
            isKeepRunning = false;

            if (mReadNmeaTask != null) {
                mReadNmeaTask.disconnect();
                mReadNmeaTask = null;
            }

            mBluetoothDeviceSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            mBluetoothDeviceSocket = null;
            if (notify) {
                mListener.onDisConnected();
            }
        }
    }

    public void disconnect() {
        disconnect(true);
    }

    public void silenceDisconnect() {
        disconnect(false);
    }

    public BluetoothDevice getDevice() {
        return mBluetoothDevice;
    }

    public int getRetries() {
        return mRetries;
    }

    public boolean isConnected() {
        if ((mBluetoothDeviceSocket != null) && mBluetoothDeviceSocket.isConnected()) {
            mSirfWraper.sendNmeaCommand(getString(R.string.sirf_nmea_rmc_on));
            return true;
        } else {
            return false;
        }
    }
}
