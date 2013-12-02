package com.gracecode.android.btgps.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.helper.BroadcastHelper;
import com.gracecode.android.btgps.helper.UIHelper;
import com.gracecode.android.btgps.serivce.ConnectService;
import com.gracecode.android.btgps.ui.fragment.ControlFragment;
import com.gracecode.android.btgps.ui.fragment.PrefControlFragment;

public class MainActivity extends BaseActivity {
    private static final int REQUEST_ENABLE_BLUETOOTH = 999;
    private BluetoothAdapter mBluetoothAdapter;
    private ControlFragment mControlFragment;
    private PrefControlFragment mPrefControlFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        if (!mBluetoothGPS.isSupported()) {
            // The device does not support Bluetooth
            UIHelper.showToast(MainActivity.this, getString(R.string.no_bluetooth));
            finish();
        }

//        mControlFragment = new ControlFragment();

        mPrefControlFragment = new PrefControlFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mPrefControlFragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mBluetoothGPS.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder instanceof ConnectService.SimpleBinder) {
                ConnectService.SimpleBinder binder = (ConnectService.SimpleBinder) iBinder;
                if (binder.isConnected()) {
                    mPrefControlFragment.markConnected();
                } else {
                    mPrefControlFragment.markDisConnected();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        bindService(BroadcastHelper.getConnectServerIntent(MainActivity.this),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(connection);
    }
}
