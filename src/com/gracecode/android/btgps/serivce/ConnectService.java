package com.gracecode.android.btgps.serivce;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.helper.BroadcastHelper;
import com.gracecode.android.btgps.helper.UIHelper;
import com.gracecode.android.btgps.ui.MainActivity;
import com.gracecode.android.btgps.util.Logger;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.provider.HeadingProvider;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.SatelliteInfoProvider;
import net.sf.marineapi.provider.event.HeadingEvent;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.ProviderListener;
import net.sf.marineapi.provider.event.SatelliteInfoEvent;

import java.io.IOException;

public class ConnectService extends Service {
    public static final String ACTION_CONNECT = "com.gracecode.btgps.service.connect";
    public static final String ACTION_DISCONNECT = "com.gracecode.btgps.service.disconnect";
    private static final int NOTIFY_ID = 0x314159;

    private ConnectThread mConnectThread;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotification;

    private class SatelliteInfoListener implements ProviderListener<SatelliteInfoEvent> {
        @Override
        public void providerUpdate(SatelliteInfoEvent satelliteInfoEvent) {
            //Logger.v(satelliteInfoEvent.toString());
            // ...
        }
    }


    private class PositionListener implements ProviderListener<PositionEvent> {
        private Location convertPositionEvent2Location(PositionEvent event) {
            Location location = new Location(BluetoothGPS.GPS_PROVIDER);

            // position
            location.setLongitude(event.getPosition().getLongitude());
            location.setLatitude(event.getPosition().getLatitude());
            location.setAltitude(event.getPosition().getAltitude());

            // movement
            location.setBearing((float) (event.getCourse() * 1f));
            location.setSpeed((float) (event.getSpeed() * 0.514444)); // From knot to m/s

            // signal
            // convert km/h to m/s
            location.setTime(System.currentTimeMillis());
            location.setAccuracy(event.getFixQuality().toInt());

            return location;
        }

        @Override
        public void providerUpdate(PositionEvent event) {
            Location location = convertPositionEvent2Location(event);
            Logger.v("Get current location is " + location.toString());
            BroadcastHelper.sendLocationUpdateBroadcast(ConnectService.this, location);
        }
    }

    private class HeadingListener implements ProviderListener<HeadingEvent> {
        @Override
        public void providerUpdate(HeadingEvent headingEvent) {
            //Logger.v(headingEvent.toString());
        }
    }


    public class SimpleBinder extends Binder {
        public boolean isConnected() {
            return (mConnectThread != null) && mConnectThread.isConnected();
        }

        public BluetoothDevice getDevice() {
            if (mConnectThread != null) {
                return mConnectThread.getDevice();
            }

            return null;
        }
    }

    private SimpleBinder mSimpleBinder = new SimpleBinder();

    private class ConnectThread extends Thread implements SentenceListener {
        private BluetoothDevice mBluetoothDevice;
        private BluetoothSocket mBluetoothDeviceSocket;
        private SentenceReader mSentenceReader;

        public ConnectThread(BluetoothDevice device) {
            mBluetoothDevice = device;
        }

        @Override
        public void run() {
            try {
                mBluetoothDeviceSocket =
                        mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(BluetoothGPS.getUUID());

                // Start connect
                mBluetoothDeviceSocket.connect();
                if (mBluetoothDeviceSocket.isConnected()) {
                    mSentenceReader = new SentenceReader(mBluetoothDeviceSocket.getInputStream());
                    mSentenceReader.addSentenceListener(ConnectThread.this);

                    new SatelliteInfoProvider(mSentenceReader).addListener(new SatelliteInfoListener());
                    new PositionProvider(mSentenceReader).addListener(new PositionListener());
                    new HeadingProvider(mSentenceReader).addListener(new HeadingListener());

                    mSentenceReader.start();
                    notifyRunning();
                    BroadcastHelper.sendDeviceConnectedBroadcast(ConnectService.this, mBluetoothDevice);
                    Logger.i("Bluetooth device '" + mBluetoothDevice.getName() + "' is connected.");
                } else {
                    throw new IOException("Socket not ready or maybe is not NMEA device.");
                }
            } catch (IOException e) {
                BroadcastHelper.sendDeviceConnectFailedBroadcast(ConnectService.this, mBluetoothDevice);
                Logger.e(e.getMessage());
                close();
            }
        }


        public boolean isConnected() {
            return (mBluetoothDeviceSocket != null) && mBluetoothDeviceSocket.isConnected();
        }

        public void close() {
            if (mSentenceReader != null) {
                mSentenceReader.stop();
                mSentenceReader = null;
            }

            if (mBluetoothDeviceSocket != null) {
                try {
                    mBluetoothDeviceSocket.close();
                    Logger.i("Bluetooth device '" + mBluetoothDevice.getName() + "' is closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mBluetoothDeviceSocket = null;
                }
            }

            clearNotification();
        }


        @Override
        public void readingPaused() {
            Logger.w("Sentence reader is paused.");
        }

        @Override
        public void readingStarted() {
            Logger.i("Sentence reader is started.");
        }

        @Override
        public void readingStopped() {
            Logger.e("Sentence reader is stopped.");
        }

        @Override
        public void sentenceRead(SentenceEvent event) {
//            Logger.v(sentenceEvent.toString());
        }

        public String getDeviceName() {
            return mBluetoothDevice.getName();
        }

        public BluetoothDevice getDevice() {
            return mBluetoothDevice;
        }
    }


    private BroadcastReceiver mBluetoothDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ConnectService.ACTION_CONNECT:
                    try {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothGPS.EXTRA_DEVICE);
                        if (device != null) {
                            connect(device);
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    break;

                case ConnectService.ACTION_DISCONNECT:
                case BluetoothGPS.ACTION_DEVICE_CONNECT_FAILED:
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    disconnect();
                    BroadcastHelper.sendRemoveProviderBroadcast(ConnectService.this, LocationManager.GPS_PROVIDER);
                    clearNotification();

                    if (intent.getAction().equals(BluetoothGPS.ACTION_DEVICE_CONNECT_FAILED)) {
                        UIHelper.showToast(ConnectService.this, getString(R.string.connect_failed));
                    }
                    break;
            }
        }
    };

    private void connect(BluetoothDevice device) {
        if (mConnectThread != null) {
            Logger.w("Close '" + mConnectThread.getDeviceName() + "' for connect new device.");
            disconnect();
        }

        Logger.i("Connecting '" + device.getName() + "'");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

    }

    private void disconnect() {
        if (mConnectThread != null) {
            mConnectThread.close();
            mConnectThread = null;
        }

        BroadcastHelper.sendDeviceDisconnectedBroadcast(ConnectService.this);
    }

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
                .addAction(R.drawable.ic_stop, getString(R.string.stop),
                        getStopPendingIntent());
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

    public void clearNotification() {
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
