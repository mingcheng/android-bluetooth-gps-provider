package com.gracecode.android.btgps.ui.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.*;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.common.helper.IntentHelper;

import java.util.Set;

public class ControlFragment extends PreferenceFragment
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
        mSharedPreferences.registerOnSharedPreferenceChangeListener(ControlFragment.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(ControlFragment.this);
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        try {
            switch (preference.getKey()) {
                case BluetoothGPS.PREF_START_GPS:
                    if (mSharedPreferences.getBoolean(BluetoothGPS.PREF_START_GPS, true)) {
                        mBluetoothGPS.connect(getBluetoothDeviceFromPref());
                    } else {
                        mBluetoothGPS.disconnect();
                    }
                    break;

                case BluetoothGPS.PREF_ABOUT:

                    break;

                case BluetoothGPS.PREF_FEEDBACK:
                    try {
                        PackageInfo info = getActivity().getPackageManager().getPackageInfo(
                                getActivity().getPackageName(), PackageManager.GET_META_DATA);

                        String title = getString(R.string.feedback, getString(R.string.app_name), info.versionName);
                        IntentHelper.sendMail(getActivity(),
                                new String[]{getString(R.string.mail)}, title, null);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
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

            case BluetoothGPS.PREF_MOCK_GPS_NAME:
                updateProviderName();
                break;
        }

        getActivity().onContentChanged();
    }


    @Override
    public void onResume() {
        super.onResume();

        updateDevicePreferenceList();
        updateDevicePreferenceSummary();
        updateMaxConnRetries();
        updateProviderName();
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

        if (entryValues.length <= 0) {
            prefDevices.setEnabled(false);
        } else {
            prefDevices.setEnabled(true);
        }
    }

    /**
     * Update selected device name.
     */
    private void updateDevicePreferenceSummary() {
        String deviceName = "";
        ListPreference prefDevices = (ListPreference) findPreference(BluetoothGPS.PREF_BLUETOOTH_DEVICE);

        if (prefDevices != null) {
            try {
                deviceName = getBluetoothDeviceFromPref().getName();
            } catch (NullPointerException e) {
                deviceName = getString(android.R.string.unknownName);
            } finally {
                prefDevices.setSummary(getString(R.string.pref_bluetooth_device_summary, deviceName));
            }
        }
    }


    private void updateMaxConnRetries() {
        Preference prefMaxConnRetries = findPreference(BluetoothGPS.PREF_CONNECTION_RETRIES);
        String maxConnRetries = mSharedPreferences.getString(BluetoothGPS.PREF_CONNECTION_RETRIES,
                getString(R.string.defaultConnectionRetries));
        prefMaxConnRetries.setSummary(getString(R.string.pref_connection_retries_summary, maxConnRetries));
    }


    private BluetoothDevice getBluetoothDeviceFromPref() {
        String deviceAddress = mSharedPreferences.getString(
                BluetoothGPS.PREF_BLUETOOTH_DEVICE, getString(android.R.string.unknownName));
        return mBluetoothGPS.getRemoteDevice(deviceAddress);
    }


    private void updateProviderName() {
        Preference prefMaxConnRetries = findPreference(BluetoothGPS.PREF_MOCK_GPS_NAME);
        String prodiverName = mSharedPreferences.getString(BluetoothGPS.PREF_MOCK_GPS_NAME,
                getString(R.string.defaultMockGpsName));
        prefMaxConnRetries.setSummary(getString(R.string.pref_mock_gps_name_summary, prodiverName));
    }
}
