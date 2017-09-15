package net.tscloud.kegstatusclient;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ntt.customgaugeview.library.GaugeView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends AppCompatActivity implements
        GaugeFragment.OnGaugeFragmentInteractionListener {
    private static final String TAG = "MainActivity";

    // ConfigurationBuilder used by twitter routines
    private ConfigurationBuilder mCb;
    private Configuration mConfig;
    Twitter mTwitter;
    TwitterStream mTwitterStream;

    // Need a ref to Activity to call runOnUiThread()
    Activity mCtx;

    // should not make this member var
    private TextView mTextViewKegStatus;
    private GaugeView mGaugeViewTemp;
    private GaugeView mGaugeViewHumidity;

    // Command string
    private static final String CMD_STRING = "posttemp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");

        setContentView(R.layout.activity_main_gauge);

        // setup for TwitterStream to make updates to UI
        mCtx = this;

        // can only do this once
        mCb = new ConfigurationBuilder();
        mCb.setDebugEnabled(true);
        mCb.setOAuthConsumerKey("21LTjHWieVX1ZZh9F2Ihavoff");
        mCb.setOAuthConsumerSecret("nz7DnSK3fXDwlSvvMTSOOzGUVU7cRQtJM51oO6A1XvShwZEYY5");
        mCb.setOAuthAccessToken("903810597942329346-du8NcfRGcJgpo3GOenoQ9Xo6h3ELdDn");
        mCb.setOAuthAccessTokenSecret("xUau6SdzMua0XDASgbXhOi1e6eo3JPBNgljyOUg2qsszB");

        mConfig = mCb.build();
        mTwitter = new TwitterFactory(mConfig).getInstance();
        mTwitterStream = new TwitterStreamFactory(mConfig).getInstance();

        final Button btnGetKegStatus = (Button)findViewById(R.id.btnGetKegStatus);
        mTextViewKegStatus = (TextView)findViewById(R.id.textViewKegStatus);

        btnGetKegStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postRequest(CMD_STRING);
            }
        });

        //TEST
        mGaugeViewTemp = (GaugeView) findViewById(R.id.gauge_view_temp);
        mGaugeViewHumidity = (GaugeView) findViewById(R.id.gauge_view_hum);

        /*
        gaugeViewTemp.setShowRangeValues(true);
        gaugeViewTemp.setTargetValue(0);
        gaugeViewHum.setShowRangeValues(true);
        gaugeViewHum.setTargetValue(0);

        gaugeViewTemp.setTargetValue(30);
        gaugeViewHum.setTargetValue(20);
        */

        // I think this wants to be done here as apposed to a data set time
        mGaugeViewTemp.setShowRangeValues(true);
        mGaugeViewTemp.setTargetValue(30);
        mGaugeViewHumidity.setShowRangeValues(true);
        mGaugeViewHumidity.setTargetValue(0);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart() called");
        // try and read temp/hum tweet - do this only once
        receiveReply();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop() called -- cleanup TwitterStream");

        new KillTwitterStream().execute();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(TAG, "onFragmentInteraction() called");
    }

    private void postRequest(String aStatusChange) {
        Log.d(TAG, "postRequest() called");

        // AsyncTask to sent command tweet
        new SendTweet(aStatusChange).execute();
    }

    // Need a method to set the TextView
    private void setStatusText(final String aText) {
        Log.d(TAG, "setStatusText() called");

        mTextViewKegStatus.setText(aText);
    }

    // Need a method to set the TextView
    private void setGauges(final String aText) {
        Log.d(TAG, "setGauges() called");

        //parse out values
        final String TEMP_IND = "Temp: ";
        final String HUM_IND = "Humidity: ";
        final int dataLen = 5;
        final int posTemp = TEMP_IND.length() + 1;
        final int posHum = aText.indexOf(HUM_IND) + HUM_IND.length();

        String tempOut= aText.substring(posTemp, posTemp + dataLen);
        String humidityOut= aText.substring(posHum, posHum + dataLen);
        Log.d(TAG, "1)--" + tempOut + "--");
        Log.d(TAG, "2)--" + humidityOut + "--");

        // Set Gauges
        mGaugeViewTemp.setShowRangeValues(true);
        mGaugeViewTemp.setTargetValue(Float.parseFloat(tempOut));
        mGaugeViewHumidity.setShowRangeValues(true);
        mGaugeViewHumidity.setTargetValue(Float.parseFloat(humidityOut));
    }

    private void receiveReply() {
        Log.d(TAG, "setting up TwitterStream");

        StatusListener listener = new StatusListener() {

            @Override
            public void onStatus(final Status status) {
                Log.d(TAG, "@" + status.getUser().getScreenName() + " - " + status.getText());

                String initialInd = "Get Keg Status";
                String noRespInd = "Server did not respond";
                String resultInd = "Kegstatus --";

                String reply;

                if (status.getText().startsWith(resultInd)) {
                    // set reply & trim off the indicator
                    reply = status.getText().substring(resultInd.length());

                    try {
                        // delete the tweet - don't want to keep these in our home timeline
                        mTwitter.destroyStatus(status.getId());
                        Log.d(TAG, "Successfully deleted tweet");
                    } catch (TwitterException te) {
                        te.printStackTrace();
                        Log.d(TAG, "Failed to delete tweet: " + te.getMessage());
                        reply = "TwitterException received on tweet delete";
                    }

                    // have to use a final String for UI updates
                    final String realReply = reply;

                    // set the TextView
                    mCtx.runOnUiThread(new Runnable() {
                        public void run() {
                            setStatusText(realReply);
                            setGauges(realReply);
                        }
                    });
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                Log.d(TAG, "Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                Log.d(TAG, "Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                Log.d(TAG, "Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                Log.d(TAG, "Got StallWarning: " + warning.getMessage());
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };

        FilterQuery fq = new FilterQuery();
        String keywords[] = {"Kegstatus --"};

        fq.track(keywords);

        mTwitterStream.addListener(listener);
        mTwitterStream.filter(fq);
    }

    private class SendTweet extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "SendTweet";

        String mStatusChange;

        SendTweet(String aStatusChange) {
            mStatusChange = aStatusChange;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            twitter4j.Status status = null;

            try {
                status = mTwitter.updateStatus(mStatusChange);
            }
            catch (TwitterException e) {
                Log.d(TAG, "Caught TwitterException trying to tweet: " + e.getMessage());
            }

            if (status != null) {
                Log.d(TAG, "Successfully updated the status to [" + status.getText() + "].");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class KillTwitterStream extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "KillTwitterStream";

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Killing TwitterStream");

            mTwitterStream.cleanUp();
            mTwitterStream.shutdown();

            return null;
        }
    }
}
