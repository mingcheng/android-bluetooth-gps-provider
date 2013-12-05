package com.gracecode.android.btgps.serivce;

import android.app.Service;
import android.content.*;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.common.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class RecordService extends Service {
    private BluetoothGPS mBluetoothGPS;
    private SharedPreferences mSharedPreferences;
    private BufferedWriter mTrackRecordsWriter;
    private File mTrackFile;

    private BroadcastReceiver mNMEAReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothGPS.ACTION_UPDATE_SENTENCE:
                    String sentence = intent.getStringExtra(BluetoothGPS.EXTRA_SENTENCE);
                    try {
                        mTrackRecordsWriter.write(sentence + "\n");
                        Logger.v(sentence);
                    } catch (IOException | RuntimeException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };


    private boolean isExternalStoragePresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private File getExternalStoragePath() {
        if (isExternalStoragePresent()) {
            File directory = Environment.getExternalStorageDirectory();
            if (directory.exists() && directory.isDirectory()) {
                return directory;
            }
        }

        return Environment.getDataDirectory();
    }


    private File getRecordFile() throws IOException {
        java.text.DateFormat mDateFormat = DateFormat.getDateFormat(RecordService.this);
        java.text.DateFormat mTimeFormat = DateFormat.getTimeFormat(RecordService.this);
        Date now = new Date(System.currentTimeMillis());
        String filename = mDateFormat.format(now) + "_" + mTimeFormat.format(now);

        File recordFile = new File(getRecordDirectory(), filename + ".nmea");
        if (!recordFile.exists()) {
            recordFile.createNewFile();
        }

        Logger.v("The record file path is " + recordFile.getAbsolutePath());
        return recordFile;
    }


    private File getRecordDirectory() {
        String trackFileDirectoryName =
                mSharedPreferences.getString(BluetoothGPS.PREF_TRACK_FILE_DIR,
                        getString(R.string.defaultTrackFileDirectory));

        File trackFileDirectory = new File(getExternalStoragePath(), trackFileDirectoryName);
        if (!trackFileDirectory.exists() || !trackFileDirectory.isDirectory()) {
            trackFileDirectory.delete();
        }

        trackFileDirectory.mkdirs();
        Logger.v("The record directory is " + trackFileDirectory.getAbsolutePath());
        return trackFileDirectory;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBluetoothGPS = (BluetoothGPS) getApplication();
        mSharedPreferences = mBluetoothGPS.getSharedPreferences();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            mTrackFile = getRecordFile();
            if (mTrackFile.canWrite()) {
                mTrackRecordsWriter = new BufferedWriter(new FileWriter(mTrackFile));
            } else {
                throw new IOException("Track file is not writeable.");
            }

        } catch (IOException | RuntimeException e) {
            Logger.e(e.getMessage());
        }

        registerReceiver(mNMEAReceiver, new IntentFilter(BluetoothGPS.ACTION_UPDATE_SENTENCE));
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        try {
            mTrackRecordsWriter.flush();
            mTrackRecordsWriter.close();

            // delete track file if doesn't record anything.
            if (mTrackFile.length() == 0) {
                mTrackFile.delete();
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        unregisterReceiver(mNMEAReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
