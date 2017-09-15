package net.tscloud.kegstatusclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class MainActivityOrig2 extends AppCompatActivity {
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
        /* TEST
        mCb = new ConfigurationBuilder();
        mCb.setDebugEnabled(true);
        mCb.setOAuthConsumerKey("21LTjHWieVX1ZZh9F2Ihavoff");
        mCb.setOAuthConsumerSecret("nz7DnSK3fXDwlSvvMTSOOzGUVU7cRQtJM51oO6A1XvShwZEYY5");
        mCb.setOAuthAccessToken("903810597942329346-du8NcfRGcJgpo3GOenoQ9Xo6h3ELdDn");
        mCb.setOAuthAccessTokenSecret("xUau6SdzMua0XDASgbXhOi1e6eo3JPBNgljyOUg2qsszB");

        mConfig = mCb.build();
        mTwitter = new TwitterFactory(mConfig).getInstance();
        */

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
        //SendTweet task = new SendTweet(aStatusChange);
        //task.execute();
        ReadHttpUrl task = new ReadHttpUrl("https://www.dropbox.com/s/bppsvfelpembf7v/test.txt");
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
                        if (status.getText().startsWith(resultInd)) {
                            Log.d(TAG, "@" + status.getUser().getScreenName() + " - " + status.getText());
                            reply = status.getText();
                            // delete the tweet - don't want to keep these in out home timeline
                            mTwitter.destroyStatus(status.getId());
                            break;
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

    private class ReadHttpUrl extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "ReadHttpUrl";

        String mUrl;

        public ReadHttpUrl(String aUrl) {
            mUrl = aUrl;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Read provided URL as bunch of Strings");

            try{
                URL url = new URL(mUrl);
                //First open the connection
                HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(60000); // timing out in a minute

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                //t=(TextView)findViewById(R.id.TextView1); // ideally do this in onCreate()
                String str;
                while ((str = in.readLine()) != null) {
                    Log.d(TAG, str);
                }
                in.close();
            } catch (Exception e) {
                Log.d("MyTag",e.toString());
            }
            /*
            try {

                String url = mUrl;

                URL obj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
                conn.setReadTimeout(5000);
                conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                conn.addRequestProperty("User-Agent", "Mozilla");
                conn.addRequestProperty("Referer", "google.com");

                System.out.println("Request URL ... " + url);

                boolean redirect = false;

                // normally, 3xx is redirect
                int status = conn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
                }

                System.out.println("Response Code ... " + status);

                if (redirect) {

                    // get redirect url from "location" header field
                    String newUrl = conn.getHeaderField("Location");

                    // get the cookie if need, for login
                    String cookies = conn.getHeaderField("Set-Cookie");

                    // open the new connnection again
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setRequestProperty("Cookie", cookies);
                    conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                    conn.addRequestProperty("User-Agent", "Mozilla");
                    conn.addRequestProperty("Referer", "google.com");

                    System.out.println("Redirect to URL : " + newUrl);

                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer html = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    html.append(inputLine);
                }
                in.close();

                System.out.println("URL Content... \n" + html.toString());
                System.out.println("Done");

            } catch (Exception e) {
                e.printStackTrace();
            }
            */

            return null;
        }
    }
}
