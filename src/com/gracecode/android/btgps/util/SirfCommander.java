package com.gracecode.android.btgps.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import com.gracecode.android.btgps.BluetoothGPS;
import com.gracecode.android.btgps.R;
import com.gracecode.android.btgps.helper.SirfHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SirfCommander {
    private SharedPreferences sharedPreferences;
    private Context mContext;
    private OutputStream mStream;
    private OutputStreamWriter mStreamWriter;
    private ExecutorService mRunCommandPool;
    private OutputStreamWriter mNmeaCommandWriter;

    public SirfCommander(Context context, OutputStream stream) {
        mContext = context;
        mStream = stream;
        mStreamWriter = new OutputStreamWriter(stream);
        mRunCommandPool = Executors.newSingleThreadExecutor();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private String getString(int resId) {
        return mContext.getString(resId);
    }

    public void enableNmeaGGA(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_gga_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_gga_off));
        }
    }


    public void enableNmeaRMC(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_rmc_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_rmc_off));
        }
    }

    public void enableNmeaGLL(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_gll_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_gll_off));
        }
    }

    public void enableNmeaVTG(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_vtg_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_vtg_off));
        }
    }

    public void enableNmeaGSA(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_gsa_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_gsa_off));
        }
    }

    public void enableNmeaGSV(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_gsv_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_gsv_off));
        }
    }

    public void enableNmeaZDA(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_zda_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_zda_off));
        }
    }

    public void enableSBAS(boolean enable) {
        if (enable) {
            sendNmeaCommand(getString(R.string.sirf_nmea_sbas_on));
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_sbas_off));
        }
    }

    public void enableNMEA(boolean enable) {
        if (enable) {
            int gll = (sharedPreferences.getBoolean(BluetoothGPS.PREF_SIRF_ENABLE_GLL, false)) ? 1 : 0;
            int vtg = (sharedPreferences.getBoolean(BluetoothGPS.PREF_SIRF_ENABLE_VTG, false)) ? 1 : 0;
            int gsa = (sharedPreferences.getBoolean(BluetoothGPS.PREF_SIRF_ENABLE_GSA, false)) ? 5 : 0;
            int gsv = (sharedPreferences.getBoolean(BluetoothGPS.PREF_SIRF_ENABLE_GSV, false)) ? 5 : 0;
            int zda = (sharedPreferences.getBoolean(BluetoothGPS.PREF_SIRF_ENABLE_ZDA, false)) ? 1 : 0;
            int mss = 0;
            int epe = 0;
            int gga = 1;
            int rmc = 1;
            String command = String.format(
                    getString(R.string.sirf_bin_to_nmea_38400_alt),
                    gga, gll, gsa, gsv, rmc, vtg, mss, epe, zda);
            sendSirfCommand(command);
        } else {
            sendNmeaCommand(getString(R.string.sirf_nmea_to_binary));
        }
    }

    public void enableStaticNavigation(boolean enable) {
        boolean isInNmeaMode = sharedPreferences.getBoolean(BluetoothGPS.PREF_SIRF_ENABLE_NMEA, true);
        if (isInNmeaMode) {
            enableNMEA(false);
        }
        if (enable) {
            sendSirfCommand(getString(R.string.sirf_bin_static_nav_on));
        } else {
            sendSirfCommand(getString(R.string.sirf_bin_static_nav_off));
        }
        if (isInNmeaMode) {
            enableNMEA(true);
        }
    }


    /**
     * Sends a NMEA sentence to the bluetooth GPS.
     *
     * @param sentence the NMEA sentence without the first "$", the last "*" and the checksum.
     */
    public void sendNmeaCommand(String sentence) {
        String command = String.format((Locale) null, "$%s*%X\r\n", sentence, SirfHelper.computeChecksum(sentence));
        sendPackagedNmeaCommand(command);
    }


    /**
     * Sends a SIRF III binary command to the bluetooth GPS.
     *
     * @param payload an hexadecimal string representing the payload of the binary command
     *                (i.e. without <em>Start Sequence</em>, <em>Payload Length</em>, <em>Message Checksum</em> and <em>End Sequence</em>).
     */
    public void sendSirfCommand(String payload) {
        String command = SirfHelper.createSirfCommandFromPayload(payload);
        sendPackagedSirfCommand(command);
    }

    /**
     * Sends a NMEA sentence to the bluetooth GPS.
     *
     * @param command the complete NMEA sentence (i.e. $....*XY where XY is the checksum).
     */
    private void sendPackagedNmeaCommand(final String command) {
        mRunCommandPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mStreamWriter.write(command);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    SystemClock.sleep(500);
                    Logger.d("Sent NMEA sentence: " + command);
                }
            }
        });
    }

    /**
     * Sends a SIRF III binary command to the bluetooth GPS.
     *
     * @param commandHexa an hexadecimal string representing a complete binary command
     *                    (i.e. with the <em>Start Sequence</em>, <em>Payload Length</em>, <em>Payload</em>, <em>Message Checksum</em> and <em>End Sequence</em>).
     */
    private void sendPackagedSirfCommand(final String commandHexa) {
        final byte[] command = SirfHelper.genSirfCommand(commandHexa);
        mRunCommandPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mStreamWriter.write(Arrays.toString(command));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    SystemClock.sleep(500);
                    Logger.d("Sent SIRF sentence: " + commandHexa);
                }
            }
        });
    }

    public void close() {
        try {
            mStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mRunCommandPool.shutdown();
        }
    }
}
