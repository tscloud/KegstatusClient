package net.tscloud.kegstatusclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // ConfigurationBuilder used by twitter routines
    private ConfigurationBuilder mCb;
    private Configuration mConfig;
    Twitter mTwitter;

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
        SendTweet task = new SendTweet(aStatusChange);
        task.execute();
    }

    // Need a method to set the TextView
    private void setStatusText(String aText) {
        mTextViewKegStatus.setText(aText);
    }

    private void receiveReply() {
        ReadHomeTimeLine task = new ReadHomeTimeLine();
        task.execute();
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

            // try and read temp/hum tweet
            receiveReply();
        }
    }

    private class ReadHomeTimeLine extends AsyncTask<Void, Void, String> {
        private static final String TAG = "ReadHomeTimeLine";

        ReadHomeTimeLine() {}

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String initialInd = "Get Keg Status";
            String noRespInd = "Server did not respond";
            String resultInd = "Kegstatus --";
            Date checkDate = null;

            String reply = initialInd;

            try {
                // gets Twitter instance with default credentials
                User user = mTwitter.verifyCredentials();
                // try max 3 times to get the result we're interested in
                for (int i=0; i<3; i++) {
                    // get home timeline
                    List<twitter4j.Status> statuses = mTwitter.getHomeTimeline();
                    Log.d(TAG, "Showing @" + user.getScreenName() + "'s home timeline.");
                    for (twitter4j.Status status : statuses) {
                        Log.d(TAG, "@" + status.getUser().getScreenName() + " - " + status.getText());
                        if (status.getText().startsWith(resultInd)) {
                            if (checkDate == null) {
                                reply = status.getText();
                                checkDate = status.getCreatedAt();
                                // delete the tweet - don't want to keep these in out home timeline
                                mTwitter.destroyStatus(status.getId());
                            }
                            else if (checkDate.before(status.getCreatedAt())) {
                                    reply = status.getText();
                                    checkDate = status.getCreatedAt();
                                    // delete the tweet - don't want to keep these in out home timeline
                                    mTwitter.destroyStatus(status.getId());
                            }
                        }
                    }

                    if (reply.contains(resultInd)) {
                        // we got what we're interested on -> get out
                        reply = reply.substring(resultInd.length());
                        break;
                    }
                    else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Huh? Thread.sleep prob");
                        }
                    }
                }

                if (reply.equals(initialInd)) {
                    reply = noRespInd;
                }

            } catch (TwitterException te) {
                te.printStackTrace();
                Log.d(TAG, "Failed to get timeline: " + te.getMessage());
                reply = "TwitterException received";
            }

            return reply;
        }

        @Override
        protected void onPostExecute(String aResultStatus) {
            super.onPostExecute(aResultStatus);

            // update TextView
            setStatusText(aResultStatus);
        }
    }
}
