package com.gracecode.android.btgps.ui.fragment;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;

public class StatusFragment extends Fragment {

    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothGPS.ACTION_UPDATE_LOCATION:
                    break;

                case BluetoothGPS.ACTION_DEVICE_CONNECT_FAILED:
                    break;

                case BluetoothGPS.ACTION_UPDATE_SENTENCE:
                    ((TextView) getView()).setText(intent.getStringExtra(BluetoothGPS.EXTRA_SENTENCE));
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mStatusReceiver, BluetoothGPS.getIntentFilter());
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mStatusReceiver);
    }
}
