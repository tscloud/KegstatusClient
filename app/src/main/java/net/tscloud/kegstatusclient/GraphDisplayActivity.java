package net.tscloud.kegstatusclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static net.tscloud.kegstatusclient.R.id.graph1;

public class GraphDisplayActivity extends AppCompatActivity {
    private static final String TAG = "GraphDisplayActivity";

    private static final String FILE_LOC =
            //"https://dl.dropboxusercontent.com/s/bppsvfelpembf7v/outfile.out";
            "https://dl.dropboxusercontent.com/s/iqv71mtw9vwirzy/outfile.out";
            //"https://dl.dropboxusercontent.com/s/oaylqtfy3ltll66/test.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_display);

        // 1st retrieve data
        retrieveData();
    }

    // Get data from cloud file
    private void retrieveData() {
        Log.d(TAG, "About to retrieve data");

        ReadHttpUrl task = new ReadHttpUrl(FILE_LOC);
        task.execute();
    }

    // Let's draw some graphs
    private void doGraphs(List<DataPoint[]> aFileData) {
        Log.d(TAG, "About to draw graphs");

        final GraphView g1 = (GraphView)findViewById(graph1);
        final GraphView g2 = (GraphView)findViewById(R.id.graph2);
        final TextView t1 = (TextView)findViewById(R.id.textTitle1);
        final TextView t2 = (TextView)findViewById(R.id.textTitle2);

        LineGraphSeries tempSeries = new LineGraphSeries<>(aFileData.get(0));
        LineGraphSeries humiditySeries = new LineGraphSeries<>(aFileData.get(1));
        LineGraphSeries pressureSeries = new LineGraphSeries<>(aFileData.get(2));

        // DateFormats used by LabelFormatter and title
        Calendar calendar = Calendar.getInstance();
        DateFormat graphFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        DateFormat titleFormatter = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        /* Set up title dates */
        // just use 1st DataPoint array as all arrays should be timestamp synchronized
        DataPoint[] dpArray = aFileData.get(0);
        long firstDate = (long) dpArray[0].getX(); //ok to cast double->long
        long lastDate = (long) dpArray[dpArray.length-1].getX(); //ok to cast double->long

        calendar.setTimeInMillis(firstDate);
        String titleDates = titleFormatter.format(calendar.getTime());
        calendar.setTimeInMillis(lastDate);
        titleDates = titleDates + " - " + titleFormatter.format(calendar.getTime());

        String titleText1 = getResources().getString(R.string.temperature) + " " + titleDates;
        t1.setText(titleText1);

        String titleText2 = getResources().getString(R.string.pressure) + " " + titleDates;
        t2.setText(titleText2);

        /* Set up graphs */
        //g1.addSeries(tempSeries);
        g1.addSeries(tempSeries);
        g2.addSeries(pressureSeries);

        // set date label formatter
        g1.getGridLabelRenderer().setLabelFormatter(
                new DateAsXAxisLabelFormatter(this, graphFormatter));
        g1.getGridLabelRenderer().setNumHorizontalLabels(4);

        g2.getGridLabelRenderer().setLabelFormatter(
                new DateAsXAxisLabelFormatter(this, graphFormatter));
        g2.getGridLabelRenderer().setNumHorizontalLabels(4);

        // set manual x bounds to have nice steps
        //g1.getViewport().setMinX(humiditySeries.getLowestValueX());
        //g1.getViewport().setMaxX(humiditySeries.getHighestValueX());
        //g1.getViewport().setXAxisBoundsManual(true);

        // as we use dates as labels, the human rounding to nice readable numbers
        // is not necessary
        //g1.getGridLabelRenderer().setHumanRounding(false);
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

                // Python timestamp * 1000 to get Java timestamp
                // pressure / 100 to get proper value
                try {
                    tempPoints[i] = new DataPoint(new Date(Long.parseLong(ts)*1000),
                            Double.parseDouble(temp));
                    humidityPoints[i] = new DataPoint(new Date(Long.parseLong(ts)*1000),
                            Double.parseDouble(humidity));
                    pressurePoints[i] = new DataPoint(new Date(Long.parseLong(ts)*1000),
                            Double.parseDouble(pressure)/100);
                }
                catch (Exception e) {
                    Log.d(TAG, "Number not received from file: " + e.getMessage());
                }

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
