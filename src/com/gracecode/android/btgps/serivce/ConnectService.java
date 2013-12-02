package com.gracecode.android.btgps.serivce;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.helper.BroadcastHelper;
import com.gracecode.android.btgps.thread.ConnectThread;
import com.gracecode.android.btgps.ui.MainActivity;
import com.gracecode.android.btgps.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectService extends Service {
    public static final String ACTION_CONNECT = "com.gracecode.btgps.service.connect";
    public static final String ACTION_DISCONNECT = "com.gracecode.btgps.service.disconnect";
    private static final int RUNNING_NOTIFY_ID = 0x314159;
    private static final int FAILED_NOTIFY_ID = 0x314159 * 2;

    private BluetoothGPS mBluetoothGPS;
    private ConnectThread mConnectThread;
    private SharedPreferences mSharedPreferences;
    private NotificationCompat.Builder mConnectFailedNotification;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mRunningNotification;
    private SimpleBinder mSimpleBinder = new SimpleBinder();
    private ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();
    private BluetoothDevice mBluetoothDevice;


    /**
     * Detect whether device is connected.
     *
     * @return boolean
     */
    private boolean isConnected() {
        return (mConnectThread != null) && mConnectThread.isConnected();
    }

    /**
     * ReadNmeaTask Status Changed Listener
     */
    private ConnectThread.OnStatusChangeListener mOnStatusChangedListener =
            new ConnectThread.OnStatusChangeListener() {

                private boolean isEnableProvider() {
                    return mSharedPreferences.getBoolean(BluetoothGPS.PREF_FORCE_ENABLE_PROVIDER, true);
                }

                private boolean isEnableLogger() {
                    return mSharedPreferences.getBoolean(BluetoothGPS.PREF_TRACK_RECORDING, true);
                }

                private String getProviderName() {
                    if (mSharedPreferences.getBoolean(BluetoothGPS.PREF_REPLACE_STD_GPS, false)) {
                        return LocationManager.GPS_PROVIDER;
                    }

                    return mSharedPreferences.getString(BluetoothGPS.PREF_GPS_LOCATION_PROVIDER,
                            getString(R.string.defaultMockGpsName));
                }

                @Override
                public void onConnected() {
                    BroadcastHelper.sendDeviceConnectedBroadcast(ConnectService.this, mConnectThread.getDevice());
                    if (isEnableProvider()) {
                        startService(BroadcastHelper.getProviderServerIntent(ConnectService.this, getProviderName()));
                    }

                    if (isEnableLogger()) {
                        startService(BroadcastHelper.getRecordServerIntent(ConnectService.this));
                    }

                    notifyRunning();
                    clearFailedNotification();
                }

                @Override
                public void onConnectedFailed(int retries) {
                    Logger.e("Connect error, has reconnect for " + retries + " times.");
                    notifyFailed(retries);
                }

                @Override
                public void onDisConnected() {
                    BroadcastHelper.sendDeviceDisconnectedBroadcast(ConnectService.this);
                    if (isEnableProvider()) {
                        stopService(BroadcastHelper.getProviderServerIntent(ConnectService.this, getProviderName()));
                    }

                    if (isEnableLogger()) {
                        stopService(BroadcastHelper.getRecordServerIntent(ConnectService.this));
                    }

                    mConnectThread = null;
                    mBluetoothDevice = null;
                    clearRunningNotification();
                }


                @Override
                public void onStartConnect() {
//                    clearFailedNotification();
                }

                @Override
                public void onReceivePosition(Location location) {
                    BroadcastHelper.sendLocationUpdateBroadcast(ConnectService.this, location);
                }

                @Override
                public void onReceiveSentence(String sentence) {
                    BroadcastHelper.sendSentenceUpdateBroadcast(ConnectService.this, sentence);
                }

                @Override
                public void onReadingPaused() {
                }

                @Override
                public void onReadingStarted() {
                }

                @Override
                public void onReadingStoped() {
                }
            };


    /**
     * Bluetooth Device Connect Server Binder
     */
    public class SimpleBinder extends Binder {
        public boolean isConnected() {
            return ConnectService.this.isConnected();
        }
    }


    private BroadcastReceiver mBluetoothDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ConnectService.ACTION_CONNECT:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothGPS.EXTRA_DEVICE);
                    if (device != null) {
                        connect(device);
                    }
                    break;

                case ConnectService.ACTION_DISCONNECT:
                    disconnect();
                    clearFailedNotification();
                    break;
            }
        }
    };


    synchronized private void connect(BluetoothDevice device) {
        if (mConnectThread != null) {
            silenceDisconnect();
        }

        if (mBluetoothDevice == null || !mBluetoothDevice.getAddress().equals(device.getAddress())) {
            Logger.i("Instance new thread for connect '" + device.getName() + "' device.");
            mConnectThread = new ConnectThread(ConnectService.this, device, mOnStatusChangedListener);
            mBluetoothDevice = device;
        }

        Logger.i("Connecting '" + mBluetoothDevice.getName() + "'");
        mSingleThreadExecutor.execute(mConnectThread);
    }


    synchronized private void silenceDisconnect() {
        if (mConnectThread != null) {
            mConnectThread.silenceDisconnect();
            mConnectThread = null;
        }
    }

    synchronized private void disconnect() {
        if (mConnectThread != null) {
            mConnectThread.disconnect();
            mConnectThread = null;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent intent = PendingIntent.getActivity(
                ConnectService.this, 0,
                new Intent(ConnectService.this, MainActivity.class),
                Intent.FLAG_ACTIVITY_NEW_TASK);

        mRunningNotification = new NotificationCompat.Builder(ConnectService.this)
                .setSmallIcon(R.drawable.ic_stat_running)
                .setContentTitle(getString(R.string.is_running))
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(intent)
                .setTicker(getString(R.string.is_running))
                .addAction(R.drawable.ic_stop, getString(R.string.stop),
                        getStopPendingIntent());

        mConnectFailedNotification = new NotificationCompat.Builder(ConnectService.this)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(getString(R.string.connect_failed))
                .setContentIntent(intent);

        mBluetoothGPS = (BluetoothGPS) getApplication();
        mSharedPreferences = mBluetoothGPS.getSharedPreferences();
    }


    private PendingIntent getStopPendingIntent() {
        return PendingIntent.getBroadcast(
                ConnectService.this,
                RUNNING_NOTIFY_ID,
                new Intent(ConnectService.ACTION_DISCONNECT),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private void notifyFailed(int retries) {
        mConnectFailedNotification.setNumber(retries);
        mConnectFailedNotification.setSubText("Reconnect for " + retries + " times.");
        mNotificationManager.notify(FAILED_NOTIFY_ID, mConnectFailedNotification.build());
    }

    public void notifyRunning() {
        mNotificationManager.notify(RUNNING_NOTIFY_ID, mRunningNotification.build());
    }

    public void clearRunningNotification() {
        mNotificationManager.cancel(RUNNING_NOTIFY_ID);
    }

    private void clearFailedNotification() {
        mNotificationManager.cancel(FAILED_NOTIFY_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothGPS.EXTRA_DEVICE);
            if (device != null) {
                connect(device);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        registerReceiver(mBluetoothDeviceReceiver, BluetoothGPS.getIntentFilter());
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        disconnect();

        try {
            unregisterReceiver(mBluetoothDeviceReceiver);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        mSingleThreadExecutor.shutdown();
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mSimpleBinder;
    }
}
