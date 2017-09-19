package net.tscloud.kegstatusclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GraphDisplayActivity extends AppCompatActivity {
    private static final String TAG = "GraphDisplayActivity";

    private static final String FILE_LOC =
            "https://dl.dropboxusercontent.com/s/bppsvfelpembf7v/test.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_display);

        // 1st retrieve data
        retrieveData();

        // TEST
        /*
        GraphView graph1 = (GraphView) findViewById(R.id.graph1);
        GraphView graph2 = (GraphView) findViewById(R.id.graph2);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graph1.addSeries(series);
        graph2.addSeries(series);
        */

    }

    // Get data from cloud file
    private void retrieveData() {
        Log.d(TAG, "About to retrieve data");

        ReadHttpUrl task = new ReadHttpUrl(FILE_LOC);
        task.execute();
    }

    // Let's draw some graphs
    private void doGraphs(List<DataPoint[]> aFileData) {

    }

    private class ReadHttpUrl extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "ReadHttpUrl";

        String mUrl;
        List<DataPoint[]> mDataPointList = new ArrayList<>();

        ReadHttpUrl(String aUrl) {
            mUrl = aUrl;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "ReadHttpUrl.doInBackground()");

            try{
                List<String> fileStrings = new ArrayList<>();
                URL url = new URL(mUrl);
                //First open the connection
                HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(60000); // timing out in a minute

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                //t=(TextView)findViewById(R.id.TextView1); // ideally do this in onCreate()
                String str;
                while ((str = in.readLine()) != null) {
                    Log.d(TAG, str);

                    //add the file string to the List we'll use to start hashing
                    fileStrings.add(str);
                }
                in.close();

                makeDataPointList(fileStrings);

            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }

            return null;
        }

        private void makeDataPointList(List<String> aStringList) {
            Log.d(TAG, "ReadHttpUrl.makeDataPointList");

            DataPoint[] tempPoints = new DataPoint[aStringList.size()];
            DataPoint[] humidityPoints = new DataPoint[aStringList.size()];
            DataPoint[] pressurePoints = new DataPoint[aStringList.size()];

            int tsDI, tempDI, bmpTempDI, humidityDI;
            String ts, temp, bmpTemp, humidity, pressure;
            int i = 0;

            for (String s : aStringList) {
                //new DataPoint(new Date(k), in.get(k))

                tsDI = s.indexOf(',');
                tempDI = s.indexOf(',', tsDI+1);
                bmpTempDI = s.indexOf(',', tempDI+1);
                humidityDI = s.indexOf(',', bmpTempDI+1);

                ts = s.substring(0, tsDI);
                temp = s.substring(tsDI+1, tempDI);
                bmpTemp = s.substring(tempDI+1, bmpTempDI);
                humidity = s.substring(bmpTempDI+1, humidityDI);
                pressure = s.substring(humidityDI+1);

                Log.d(TAG, "ts: " + ts + "|temp: " + temp+ "|bmpTemp: " + bmpTemp +
                        "|humidity: " + humidity + "|pressure: " + pressure);

                tempPoints[i] = new DataPoint(new Date(ts), temp);
                humidityPoints[i] = new DataPoint(new Date(ts), bmpTemp);
                pressurePoints[i] = new DataPoint(new Date(ts), pressure);

                i++;
            }

            mDataPointList.add(tempPoints);
            mDataPointList.add(humidityPoints);
            mDataPointList.add(pressurePoints);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Log.d(TAG, "ReadHttpUrl.onPostExecute");

            doGraphs(mDataPointList);
        }
    }
}
