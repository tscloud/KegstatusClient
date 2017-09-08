package net.tscloud.kegstatusclient;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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


public class MainActivityOrig extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // ConfigurationBuilder used by twitter routines
    private ConfigurationBuilder mCb;
    private Configuration mConfig;
    Twitter mTwitter;
    TwitterStream mTwitterStream;

    // should not make this member var
    private TextView mTextViewKegStatus;

    // Command string
    private static final String CMD_STRING = "posttemp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                //postRequest(textViewKegStatus.getText().toString());
                postRequest(CMD_STRING);
            }
        });
    }

    private void postRequest(String aStatusChange) {
        sendTweet task = new sendTweet(aStatusChange);
        task.execute();
    }

    // Need a method to set the TextView
    private void setStatusText(String aText) {
        mTextViewKegStatus.setText(aText);
    }

    private void receiveReply() {
        StatusListener listener = new StatusListener() {

            @Override
            public void onStatus(Status status) {
                Log.d(TAG, "@" + status.getUser().getScreenName() + " - " + status.getText());
                // set the TextView
                setStatusText(status.getText());
                mTwitterStream.cleanUp();
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

    private class sendTweet extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "SendTweet";

        String mStatusChange;

        sendTweet(String aStatusChange) {
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

            // try and read temp/hum tweet
            receiveReply();
        }
    }
}
