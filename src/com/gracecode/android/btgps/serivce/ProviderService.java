package com.gracecode.android.btgps.serivce;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.util.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ProviderService extends Service {
    private LocationManager mLocationManager;
    private Set<String> mProviderSet = new HashSet<>();

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get provider name from intent.
            String provider = intent.getStringExtra(BluetoothGPS.EXTRA_PROVIDER);

            switch (intent.getAction()) {
                case BluetoothGPS.ACTION_UPDATE_LOCATION:
                    Location location = intent.getParcelableExtra(BluetoothGPS.EXTRA_LOCATION);
                    if (location != null) {
                        setAllProviderLocation(location);
                    }
                    break;

                case BluetoothGPS.ACTION_PROVIDER_ADD:
                    if (!provider.isEmpty()) {
                        addProvider(provider);
                    }
                    break;

                case BluetoothGPS.ACTION_PROVIDER_REMOVE:
                    if (!provider.isEmpty()) {
                        removeProvider(provider);
                    }
                    break;
            }
        }
    };

    private void setAllProviderLocation(Location location) {
        Iterator<String> iterator = mProviderSet.iterator();
        while (iterator.hasNext()) {
            setProviderLocation(iterator.next(), location);
        }
    }


    private void removeAllProvider() {
        Iterator<String> iterator = mProviderSet.iterator();
        while (iterator.hasNext()) {
            removeProvider(iterator.next());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        registerReceiver(mLocationReceiver, BluetoothGPS.getIntentFilter());
    }


    public void addProvider(String provider) {
        if (!mProviderSet.contains(provider)) {
            mProviderSet.add(provider);
            mLocationManager.addTestProvider(provider,
                    false, false, false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE);

            setProviderEnabled(provider, true);
            Logger.v("Added '" + provider + "' location provider.");
        } else {
            Logger.w("The provider name '" + provider + "' is already exists, ignore.");
        }
    }


    public void removeProvider(String provider) {
        try {
            if (mProviderSet.contains(provider)) {
                mProviderSet.remove(provider);
                mLocationManager.removeTestProvider(provider);
                Logger.i("The provider '" + provider + "' is removed.");
            } else {
                Logger.i("The provider '" + provider + "' not exists.");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


    public void setProviderEnabled(String provider, boolean enabled) {
        mLocationManager.setTestProviderEnabled(provider, enabled);
        if (enabled) {
            mLocationManager.setTestProviderStatus(
                    provider, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
        }
    }


    public boolean isProviderEnabled(String provider) {
        return mLocationManager.isProviderEnabled(provider);
    }

    synchronized public void setProviderLocation(String provider, Location location) {
        if (isProviderEnabled(provider)) {
            location.setProvider(provider);

            // @see http://jgrasstechtips.blogspot.dk/2012/12/android-incomplete-location-object.html
            try {
                Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
                if (locationJellyBeanFixMethod != null) {
                    locationJellyBeanFixMethod.invoke(location);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            mLocationManager.setTestProviderLocation(provider, location);
        }
    }

    synchronized public void setProviderStatus(String provider, int status, Bundle extra) {
        if (isProviderEnabled(provider)) {
            mLocationManager.setTestProviderStatus(provider,
                    status, extra, System.currentTimeMillis());
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String provider = intent.getStringExtra(BluetoothGPS.EXTRA_PROVIDER);
            if (!provider.isEmpty()) addProvider(provider);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void clean() throws RuntimeException {
        removeAllProvider();
        unregisterReceiver(mLocationReceiver);
    }

    @Override
    public void onDestroy() {
        try {
            clean();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }


    public class SimpleBinder extends Binder {
        public ProviderService getService() {
            return ProviderService.this;
        }

        public boolean isProvideExists(String provider) {
            return mProviderSet.contains(provider);
        }

        public Set<String> getAllProviderSet() {
            return mProviderSet;
        }

        public boolean removeProvider(String provider) {
            return mProviderSet.remove(provider);
        }

        public boolean addProvider(String provider) {
            return mProviderSet.add(provider);
        }
    }

    private SimpleBinder mSimpleBinder = new SimpleBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mSimpleBinder;
    }
}
