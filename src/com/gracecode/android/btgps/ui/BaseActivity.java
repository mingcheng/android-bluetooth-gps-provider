package com.gracecode.android.btgps.ui;

import android.R;
import android.app.Activity;
import android.os.Bundle;
import com.gracecode.android.btgps.BluetoothGPS;

public class BaseActivity extends Activity {
    protected BluetoothGPS mBluetoothGPS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothGPS = (BluetoothGPS) getApplication();
        getActionBar().setIcon(R.color.transparent);
    }
}
