package com.gracecode.android.btgps.helper;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.serivce.ConnectService;
import com.gracecode.android.btgps.serivce.ProviderService;
import com.gracecode.android.btgps.serivce.RecordService;

public final class BroadcastHelper {

    public static void sendDeviceConnectedBroadcast(Context context, BluetoothDevice device) {
        context.sendBroadcast(new Intent()
                .setAction(BluetoothGPS.ACTION_DEVICE_CONNECT_SUCCESS)
                .putExtra(BluetoothGPS.EXTRA_DEVICE, device));
    }


    public static void sendDeviceDisconnectedBroadcast(Context context) {
        context.sendBroadcast(new Intent()
                .setAction(BluetoothGPS.ACTION_DEVICE_DISCONNECTED));
    }

    public static Intent getProviderServerIntent(Context context, String provider) {
        return new Intent(context, ProviderService.class)
                .putExtra(BluetoothGPS.EXTRA_PROVIDER, provider);
    }

    public static Intent getConnectServerIntent(Context context) {
        return new Intent(context, ConnectService.class);
    }

    public static Intent getRecordServerIntent(Context context) {
        return new Intent(context, RecordService.class);
    }

    public static void sendLocationUpdateBroadcast(Context context, Location location) {
        context.sendBroadcast(new Intent()
                .setAction(BluetoothGPS.ACTION_UPDATE_LOCATION)
                .putExtra(BluetoothGPS.EXTRA_LOCATION, location));
    }

    public static void sendSentenceUpdateBroadcast(Context context, String sentence) {
        context.sendBroadcast(new Intent()
                .setAction(BluetoothGPS.ACTION_UPDATE_SENTENCE)
                .putExtra(BluetoothGPS.EXTRA_SENTENCE, sentence));
    }
}
