package com.gracecode.android.btgps.task;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.provider.HeadingProvider;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.SatelliteInfoProvider;
import net.sf.marineapi.provider.event.HeadingEvent;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.ProviderListener;
import net.sf.marineapi.provider.event.SatelliteInfoEvent;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReadNmeaTask implements Runnable, SentenceListener {

    private final Context mContext;
    private final OnNmeaReadListener mOnNmeaReadListener;
    private InputStream mNmeaInputStream;
    private SentenceReader mSentenceReader;

    public interface OnNmeaReadListener {
        abstract public void onReceivePosition(Location location);

        abstract public void onReceiveSentence(String sentence);

        abstract public void onReadingPaused();

        abstract public void onReadingStarted();

        abstract public void onReadingStoped();

//        abstract public void onReceiveSatelliteInfo();
    }

    private class SatelliteInfoListener implements ProviderListener<SatelliteInfoEvent> {
        @Override
        public void providerUpdate(SatelliteInfoEvent event) {
//            Logger.v(event.toString());
//            mOnNmeaReadListener.onReceiveSatelliteInfo();
        }
    }

    private class PositionListener implements ProviderListener<PositionEvent> {
        private SimpleDateFormat mSimpleDateFormatter
                = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ssZ");

        /**
         * Convert PositionEvent's time to timestamp.
         *
         * @param event PositionEvent
         * @return UNIX Timestamp
         */
        private long getTimestampFromPositionEvent(PositionEvent event) {
            Date date = new Date();
            try {
                date = mSimpleDateFormatter.parse(
                        event.getDate().toISO8601() + " at " + event.getTime().toISO8601());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return date.getTime();
        }


        /**
         * Convert PositionEvent To Android Location
         *
         * @param event PositionEvent from NMEA
         * @return Android's Location Class
         * @see <url>https://en.wikipedia.org/wiki/Knot_(unit)</url>
         */
        private Location convertPositionEvent2Location(PositionEvent event) {
            Location location = new Location(LocationManager.GPS_PROVIDER);

            // position
            location.setLongitude(event.getPosition().getLongitude());
            location.setLatitude(event.getPosition().getLatitude());
            location.setAltitude(event.getPosition().getAltitude());

            // movement
            location.setBearing((float) (event.getCourse() * 1f));
            location.setSpeed((float) (event.getSpeed() * 0.514444f)); // convert knot to m/s

            // signal
            location.setTime(getTimestampFromPositionEvent(event));
            location.setAccuracy(event.getFixQuality().toInt());

            return location;
        }

        @Override
        public void providerUpdate(PositionEvent event) {
            mOnNmeaReadListener.onReceivePosition(convertPositionEvent2Location(event));
        }
    }


    private class HeadingListener implements ProviderListener<HeadingEvent> {
        @Override
        public void providerUpdate(HeadingEvent headingEvent) {
            //...
        }
    }


    public ReadNmeaTask(Context context, InputStream stream, OnNmeaReadListener onNmeaReadListener) {
        mContext = context;
        mOnNmeaReadListener = onNmeaReadListener;
        mNmeaInputStream = stream;
    }


    @Override
    public void run() {
        mSentenceReader = new SentenceReader(mNmeaInputStream);
        mSentenceReader.addSentenceListener(ReadNmeaTask.this);

        new SatelliteInfoProvider(mSentenceReader).addListener(new SatelliteInfoListener());
        new PositionProvider(mSentenceReader).addListener(new PositionListener());
        new HeadingProvider(mSentenceReader).addListener(new HeadingListener());

        mSentenceReader.start();
    }


    public void disconnect() {
        try {
            if (mSentenceReader != null) {
                mSentenceReader.stop();
            }

            if (mNmeaInputStream != null) {
                mNmeaInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            mSentenceReader = null;
            mNmeaInputStream = null;
        }
    }


    @Override
    public void readingPaused() {
        mOnNmeaReadListener.onReadingPaused();
    }

    @Override
    public void readingStarted() {
        mOnNmeaReadListener.onReadingStarted();
    }

    @Override
    public void readingStopped() {
        mOnNmeaReadListener.onReadingStoped();
    }

    @Override
    public void sentenceRead(SentenceEvent event) {
        Sentence sentence = event.getSentence();
        if (sentence.isValid()) {
            mOnNmeaReadListener.onReceiveSentence(sentence.toString());
        }
    }
}
