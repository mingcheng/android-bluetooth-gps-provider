package com.gracecode.android.btgps.ui.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;

import java.util.Set;

public class PrefControlFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private BluetoothGPS mBluetoothGPS;
    private SharedPreferences mSharedPreferences;

    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothGPS.ACTION_DEVICE_DISCONNECTED:
                    markDisConnected();
                    break;

                case BluetoothGPS.ACTION_DEVICE_CONNECTED:
                    markConnected();
                    break;
            }
        }
    };


    public void markDisConnected() {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(BluetoothGPS.PREF_START_GPS);
        pref.setChecked(false);
    }

    public void markConnected() {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(BluetoothGPS.PREF_START_GPS);
        pref.setChecked(true);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBluetoothGPS = (BluetoothGPS) getActivity().getApplication();

        mSharedPreferences = mBluetoothGPS.getSharedPreferences();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(PrefControlFragment.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(PrefControlFragment.this);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        switch (preference.getKey()) {
            case BluetoothGPS.PREF_START_GPS:
                if (mSharedPreferences.getBoolean(BluetoothGPS.PREF_START_GPS, true)) {
                    mBluetoothGPS.connect(getBluetoothDeviceFromPref());
                } else {
                    mBluetoothGPS.disconnect();
                }
                break;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case BluetoothGPS.PREF_BLUETOOTH_DEVICE:
                updateDevicePreferenceSummary();
                break;

            case BluetoothGPS.PREF_CONNECTION_RETRIES:
                updateMaxConnRetries();
                break;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        updateDevicePreferenceList();
        updateDevicePreferenceSummary();
        updateMaxConnRetries();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mStatusReceiver, BluetoothGPS.getIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mStatusReceiver);
    }

    /**
     * Update pared device for pref list.
     */
    private void updateDevicePreferenceList() {
        ListPreference prefDevices = (ListPreference) findPreference(BluetoothGPS.PREF_BLUETOOTH_DEVICE);
        Set<BluetoothDevice> pairedDevices = mBluetoothGPS.getPairedDevices();
        String[] entryValues = new String[pairedDevices.size()];
        String[] entries = new String[pairedDevices.size()];

        int i = 0;
        for (BluetoothDevice device : pairedDevices) {
            entryValues[i] = device.getAddress();
            entries[i] = device.getName();
            i++;
        }

        prefDevices.setEntryValues(entryValues);
        prefDevices.setEntries(entries);
    }

    /**
     * Update selected device name.
     */
    private void updateDevicePreferenceSummary() {
        String deviceName = "";
        ListPreference prefDevices = (ListPreference) findPreference(BluetoothGPS.PREF_BLUETOOTH_DEVICE);

        try {
            deviceName = getBluetoothDeviceFromPref().getName();
        } catch (NullPointerException e) {
            deviceName = getString(android.R.string.unknownName);
        }

        prefDevices.setSummary(getString(R.string.pref_bluetooth_device_summary, deviceName));
    }


    private void updateMaxConnRetries() {
        Preference prefMaxConnRetries = findPreference(BluetoothGPS.PREF_CONNECTION_RETRIES);
        String maxConnRetries = mSharedPreferences.getString(BluetoothGPS.PREF_CONNECTION_RETRIES,
                getString(R.string.defaultConnectionRetries));
        prefMaxConnRetries.setSummary(getString(R.string.pref_connection_retries_summary, maxConnRetries));
    }


    private BluetoothDevice getBluetoothDeviceFromPref() {
        String deviceAddress = mSharedPreferences.getString(BluetoothGPS.PREF_BLUETOOTH_DEVICE, null);
        return mBluetoothGPS.getRemoteDevice(deviceAddress);
    }
}
