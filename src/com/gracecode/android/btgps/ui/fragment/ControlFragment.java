package com.gracecode.android.btgps.ui.fragment;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.helper.BroadcastHelper;
import com.gracecode.android.btgps.helper.UIHelper;
import com.gracecode.android.btgps.serivce.ConnectService;
import com.gracecode.android.btgps.serivce.ProviderService;
import com.gracecode.android.btgps.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ControlFragment extends Fragment implements View.OnClickListener {
    private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;

    private ArrayAdapter<String> mBluetoothNamesAdapter;
    private Spinner mDeviceNamesSpinner;
    private Button mButtonConnectButton;
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    private BluetoothAdapter mBluetoothAdapter;
    private List<BluetoothDevice> mPairedDevices = new ArrayList<>();
    private ToggleButton mButtonProviderToggleButton;

    private class StatusReceiver extends BroadcastReceiver implements View.OnClickListener {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                case BluetoothGPS.ACTION_DEVICE_DISCONNECTED:
                case BluetoothGPS.ACTION_DEVICE_CONNECT_FAILED:
                    markDisConnected();
                    break;

                case BluetoothGPS.ACTION_DEVICE_CONNECT_SUCCESS:
                    markConnected((BluetoothDevice) intent.getParcelableExtra(BluetoothGPS.EXTRA_DEVICE));
                    break;

                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    break;

                case BluetoothGPS.ACTION_PROVIDER_ADD:
                case BluetoothGPS.ACTION_PROVIDER_REMOVE:
                    String provider = intent.getStringExtra(BluetoothGPS.EXTRA_PROVIDER);
                    if (provider.equals(LocationManager.GPS_PROVIDER)) {
                        mButtonProviderToggleButton.setChecked(
                                (intent.getAction().equals(BluetoothGPS.ACTION_PROVIDER_ADD)) ? true : false
                        );
                    }
                    break;
            }
        }

        @Override
        public void onClick(View view) {

        }
    }

    public void markConnected(BluetoothDevice device) {
        mButtonConnectButton.setEnabled(true);
        mButtonConnectButton.setText(getString(R.string.disconnect));
        mButtonConnectButton.setTag(BluetoothGPS.ACTION_DEVICE_CONNECTED);

        mButtonProviderToggleButton.setEnabled(true);
        if (device != null) {
            UIHelper.showToast(getActivity(), String.format(getString(R.string.connected), device.getName()));
        }

        updateProviderToggleButton();
    }

    private void updateProviderToggleButton() {
        if (mProviderServiceBinder != null) {
            mButtonProviderToggleButton.setChecked(
                    mProviderServiceBinder.isProvideExists(LocationManager.GPS_PROVIDER)
            );
        }
    }

    private ProviderService.SimpleBinder mProviderServiceBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder instanceof ProviderService.SimpleBinder) {
                mProviderServiceBinder = (ProviderService.SimpleBinder) iBinder;
                updateProviderToggleButton();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mProviderServiceBinder = null;
        }
    };

    public void markDisConnected() {
        mButtonConnectButton.setEnabled(true);
        mButtonConnectButton.setText(getString(R.string.connect));
        mButtonConnectButton.setTag(BluetoothGPS.ACTION_DEVICE_DISCONNECTED);

        mButtonProviderToggleButton.setChecked(false);
        mButtonProviderToggleButton.setEnabled(false);

        mDeviceNamesSpinner.setEnabled(true);
    }


    public void markConnecting() {
        mButtonConnectButton.setEnabled(false);
        mButtonConnectButton.setText(getString(R.string.connecting));

        mDeviceNamesSpinner.setEnabled(false);
        mButtonProviderToggleButton.setEnabled(false);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBluetoothNamesAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        mDeviceNamesSpinner.setAdapter(mBluetoothNamesAdapter);
        mButtonConnectButton.setOnClickListener(ControlFragment.this);
        mButtonProviderToggleButton.setOnClickListener(ControlFragment.this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, null);
        mDeviceNamesSpinner = (Spinner) view.findViewById(R.id.device_names);
        mButtonConnectButton = (Button) view.findViewById(R.id.connect);
        mButtonProviderToggleButton = (ToggleButton) view.findViewById(R.id.toggle_provider);

        TextView iconView = (TextView) view.findViewById(R.id.icon);
        iconView.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "foundation-icons.ttf"));
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            mPairedDevices.clear();
            mBluetoothNamesAdapter.clear();
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevices.add(device);
                mBluetoothNamesAdapter.add(device.getName());
            }

            mBluetoothNamesAdapter.notifyDataSetChanged();
        }

        getActivity().registerReceiver(mStatusReceiver, BluetoothGPS.getIntentFilter());
        getActivity().bindService(
                BroadcastHelper.getProviderServerIntent(getActivity(), null),
                connection,
                Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mStatusReceiver);
        getActivity().unbindService(connection);
    }


    @Override
    public void onClick(View view) {
        Intent intent = new Intent();

        switch (view.getId()) {
            case R.id.connect:
                switch ((String) mButtonConnectButton.getTag()) {
                    case BluetoothGPS.ACTION_DEVICE_CONNECTED:
                        intent.setAction(ConnectService.ACTION_DISCONNECT);
                        markDisConnected();
                        break;

                    default:
                    case BluetoothGPS.ACTION_DEVICE_DISCONNECTED:
                        try {
                            BluetoothDevice bluetoothDevice
                                    = mPairedDevices.get(mDeviceNamesSpinner.getSelectedItemPosition());

                            if (bluetoothDevice != null) {
                                Logger.i("Selected bluetooth device name is '" + bluetoothDevice.getName() + "'");
                                intent.setAction(ConnectService.ACTION_CONNECT);
                                intent.putExtra(BluetoothGPS.EXTRA_DEVICE, bluetoothDevice);
                                markConnecting();
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;

            case R.id.toggle_provider:
                if (mButtonProviderToggleButton.isChecked()) {
                    intent.setAction(BluetoothGPS.ACTION_PROVIDER_ADD);
                } else {
                    intent.setAction(BluetoothGPS.ACTION_PROVIDER_REMOVE);
                }

                intent.putExtra(BluetoothGPS.EXTRA_PROVIDER, GPS_PROVIDER);
                break;
        }

        getActivity().sendBroadcast(intent);
    }
}
