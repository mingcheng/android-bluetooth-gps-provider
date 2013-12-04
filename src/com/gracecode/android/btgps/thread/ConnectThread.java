package com.gracecode.android.btgps.thread;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.task.ReadNmeaTask;
import com.gracecode.android.btgps.util.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectThread extends Thread {
    private final Context mContext;
    private final OnStatusChangeListener mListener;
    private final BluetoothDevice mBluetoothDevice;
    private final SharedPreferences mSharedPreferences;
    private boolean isKeepRunning = true;
    public static final String DEFAULT_UUID = "00001101-0000-1000-8000-00805f9b34fb";
    private int mRetries = 0;
    private BluetoothSocket mBluetoothDeviceSocket;
    private ReadNmeaTask mReadNmeaTask;
    private ExecutorService mNotificationPool;
    private OutputStreamWriter mNmeaCommandWriter;


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

        mNotificationPool = Executors.newSingleThreadExecutor();
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

                mNmeaCommandWriter = new OutputStreamWriter(mBluetoothDeviceSocket.getOutputStream());


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

    private void disconnect(boolean notify) {
        try {
            isKeepRunning = false;

            if (mReadNmeaTask != null) {
                mReadNmeaTask.disconnect();
                mReadNmeaTask = null;
            }

            mNmeaCommandWriter.close();
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
            mNotificationPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Logger.w("write, write!");
                        mNmeaCommandWriter.write("$PSRF151,01*F");
                        mNmeaCommandWriter.flush();
                    } catch (Exception e) {
                        mBluetoothDeviceSocket = null;
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }
}
