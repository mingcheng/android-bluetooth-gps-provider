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
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.helper.BroadcastHelper;
import com.gracecode.android.btgps.thread.ConnectThread;
import com.gracecode.android.btgps.ui.MainActivity;
import com.gracecode.android.btgps.util.Logger;

public class ConnectService extends Service {
    public static final String ACTION_CONNECT = "com.gracecode.btgps.service.connect";
    public static final String ACTION_DISCONNECT = "com.gracecode.btgps.service.disconnect";
    private static final int NOTIFY_ID = 0x314159;

    private BluetoothGPS mBluetoothGPS;
    private ConnectThread mConnectThread;
    private SharedPreferences mSharedPreferences;

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
                @Override
                public void onConnected() {
                    BroadcastHelper.sendDeviceConnectedBroadcast(ConnectService.this, mConnectThread.getDevice());
                    notifyRunning();
                }

                @Override
                public void onConnectedFailed(int retries) {
                }

                @Override
                public void onDisConnected() {
                    BroadcastHelper.sendDeviceDisconnectedBroadcast(ConnectService.this);
                    clearRunningNotification();
                }

                @Override
                public void onReceivePosition(Location location) {
                }

                @Override
                public void onReceiveSentence(String sentence) {
                    Logger.v(sentence);
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
    private SimpleBinder mSimpleBinder = new SimpleBinder();


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
                    break;
            }
        }
    };


    synchronized private void connect(BluetoothDevice device) {
        if (mConnectThread != null) {
            disconnect();
        }

        Logger.i("Connecting '" + device.getName() + "'");
        mConnectThread = new ConnectThread(ConnectService.this, device, mOnStatusChangedListener);
        mConnectThread.start();
    }


    synchronized private void disconnect() {
        if (mConnectThread != null) {
            mConnectThread.disconnect();
            mConnectThread = null;
        }
    }


    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotification;

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent intent = PendingIntent.getActivity(
                ConnectService.this, 0,
                new Intent(ConnectService.this, MainActivity.class),
                Intent.FLAG_ACTIVITY_NEW_TASK);

        mNotification = new NotificationCompat.Builder(ConnectService.this)
                .setSmallIcon(R.drawable.ic_stat_running)
                .setContentTitle(getString(R.string.is_running))
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(intent)
                .setTicker(getString(R.string.is_running))
                .addAction(R.drawable.ic_stop, getString(R.string.stop),
                        getStopPendingIntent());

        mBluetoothGPS = (BluetoothGPS) getApplication();
        mSharedPreferences = mBluetoothGPS.getSharedPreferences();
    }


    private PendingIntent getStopPendingIntent() {
        return PendingIntent.getBroadcast(
                ConnectService.this,
                NOTIFY_ID,
                new Intent(ConnectService.ACTION_DISCONNECT),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    public void notifyRunning() {
        mNotificationManager.notify(NOTIFY_ID, mNotification.build());
    }

    public void clearRunningNotification() {
        mNotificationManager.cancel(NOTIFY_ID);
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

        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mSimpleBinder;
    }
}
