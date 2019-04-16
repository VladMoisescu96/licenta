package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private Button mButtonViewWeek;


    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonViewWeek = (Button) findViewById(R.id.btn_view_week);

        mButtonViewWeek.setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .build();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("HistoryAPI", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("HistoryAPI", "onConnectionFailed");
    }

    public void onConnected(@Nullable Bundle bundle) {
        Log.e("HistoryAPI", "onConnected");
    }


    @Override
    public void onClick(View v) {

        ViewWeekStepCountTask x = new ViewWeekStepCountTask();
        x.execute();
    }

    private class ViewWeekStepCountTask extends AsyncTask<Void, Void, ArrayList<HeartRate>> {
        @Override
        protected ArrayList<HeartRate> doInBackground(Void... voids) {
            return displayFor7Days();
        }

        @Override
        protected void onPostExecute(ArrayList<HeartRate> arrayList) {
            super.onPostExecute(arrayList);
            System.out.println(arrayList);
            BarChart barChart = (BarChart) findViewById(R.id.barChartId);
            ArrayList<String> theDates = new ArrayList<>();
            ArrayList<BarEntry> barEntries = new ArrayList<>();
            for (int i =0; i < 7; i++) {

                float stress;

                if (Float.parseFloat(arrayList.get(i).averageHeartRate) < 60.0) {
                    stress = 0;
                } else {
                    stress = (Float.parseFloat(arrayList.get(i).averageHeartRate) - 60);
                }
                barEntries.add(new BarEntry(i, stress));
                theDates.add(arrayList.get(i).start);
            }

            BarDataSet barDataSet = new BarDataSet(barEntries, "Stress level");

            BarData theDate = new BarData(barDataSet);
            barChart.setData(theDate);
        }
    }


    private ArrayList<HeartRate> displayFor7Days() {

        ArrayList<HeartRate> heartRates = new ArrayList<>();

        for (int i = 1; i <= 7 ; i++) {
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            System.out.println(Calendar.DAY_OF_YEAR - i);
            cal.set(Calendar.DAY_OF_YEAR, cal.get(cal.DAY_OF_YEAR) - i);
            cal.set(Calendar.HOUR_OF_DAY, 18);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long endTime = cal.getTimeInMillis();

            cal.add(Calendar.HOUR_OF_DAY, -9);
            long startTime = cal.getTimeInMillis();
            heartRates.add(displayLastWeeksData(startTime, endTime));
        }

        return heartRates;
    }
    private HeartRate displayLastWeeksData(long startTime, long endTime) {

        java.text.DateFormat dateFormat = DateFormat.getDateInstance();
        Log.e("History", "Range Start: " + dateFormat.format(startTime));
        Log.e("History", "Range End: " + dateFormat.format(endTime));

//Check how many steps were walked and recorded in the last 7 days
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_RATE_BPM, DataType.AGGREGATE_HEART_RATE_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(mGoogleApiClient, readRequest).await(1, TimeUnit.MINUTES);

        if (dataReadResult.getBuckets().size() > 0) {
            Log.e("History", "Number of buckets: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    return showDataSet(dataSet);
                }
            }
        }
//Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            Log.e("History", "Number of returned DataSets: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                return showDataSet(dataSet);
            }
        }
        return null;
    }

    private HeartRate showDataSet(DataSet dataSet) {
        Log.e("History", "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();

        HeartRate heartRate = new HeartRate();
        //for (DataPoint dp : dataSet.getDataPoints()) {
        DataPoint dp = dataSet.getDataPoints().get(0);

//        }

        heartRate.start = dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS));
        heartRate.stop = dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS));

        heartRate.averageHeartRate = dp.getValue(dp.getDataType().getFields().get(0)).toString();
        heartRate.maxHeartRate = dp.getValue(dp.getDataType().getFields().get(1)).toString();
        heartRate.minHeartRate = dp.getValue(dp.getDataType().getFields().get(2)).toString();

        return heartRate;
    }
}