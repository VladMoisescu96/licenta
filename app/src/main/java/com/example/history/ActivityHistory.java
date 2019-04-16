package com.example.history;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.example.Notifications.NotificationCenter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.api.GoogleApiClient;
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

public class ActivityHistory {

    GoogleApiClient mGoogleApiClient;

    public ActivityHistory(GoogleApiClient mGoogleApiClient) {

        this.mGoogleApiClient = mGoogleApiClient;

    }

    public void getData(FragmentActivity fragmentActivity) {

        ActivityHistory.ViewWeekHearRateCountTask hearRateCountTask = new ActivityHistory.ViewWeekHearRateCountTask(fragmentActivity);
        hearRateCountTask.execute();
    }

    public class ViewWeekHearRateCountTask extends AsyncTask<Void, Void, ArrayList<HeartRate>> {


        private FragmentActivity fragmentActivity;

        public ViewWeekHearRateCountTask(FragmentActivity fragmentActivity) {
            this.fragmentActivity = fragmentActivity;
        }

        @Override
        public ArrayList<HeartRate> doInBackground(Void... voids) {
            return displayFor7Days();
        }

        @Override
        protected void onPostExecute(ArrayList<HeartRate> arrayList) {
            super.onPostExecute(arrayList);

            ObjectMapper objectMapper = new ObjectMapper();
            HeartRateInfo heartRateInfo = new HeartRateInfo();
            heartRateInfo.setHeartRates(arrayList);

            try {
                String heartRates = objectMapper.writeValueAsString(heartRateInfo);
                NotificationCenter.getInstance().emit("heartRatesData", heartRates);

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
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
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long endTime = cal.getTimeInMillis();

            cal.add(Calendar.HOUR_OF_DAY, -23);
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
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
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
