package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.Notifications.HandlerInterface;
import com.example.Notifications.NotificationCenter;
import com.example.history.ActivityHistory;
import com.example.history.HeartRate;
import com.example.history.HeartRateHistory;
import com.example.history.HeartRateInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private Button mButtonViewWeek;


    private HeartRateHistory heartRateHistory;
    private ActivityHistory activityHistory;
    private Boolean connected;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connected = false;

        mButtonViewWeek = findViewById(R.id.btn_view_week);

        mButtonViewWeek.setOnClickListener(this);
        NotificationCenter.getInstance().on("heartRatesData",heartRatesData);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("HistoryAPI", "onConnected");
        connected = true;
    }

    @Override
    public void onClick(View v) {

        if (connected) {

            if (heartRateHistory == null) {
                heartRateHistory = new HeartRateHistory(mGoogleApiClient);
                heartRateHistory.getData(this);
            } else {
                heartRateHistory.getData(this);
            }

            if (activityHistory == null) {
                activityHistory = new ActivityHistory(mGoogleApiClient);
                activityHistory.getData(this);
            } else {
                activityHistory.getData(this);
            }
        }
    }


    HandlerInterface heartRatesData = new HandlerInterface() {
        @Override

        public void handler(String value) {

            ObjectMapper objectMapper = new ObjectMapper();
            HeartRateInfo heartRateInfo = null;

            try {
                heartRateInfo = objectMapper.readValue(value, HeartRateInfo.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (heartRateInfo != null) {

                BarChart barChart = findViewById(R.id.barChartId);
                ArrayList<String> theDates = new ArrayList<>();
                ArrayList<BarEntry> barEntries = new ArrayList<>();
                for (int i =0; i < 7; i++) {

                    float stress;

                    if (Float.parseFloat(heartRateInfo.getHeartRates().get(i).averageHeartRate) < 60) {
                        stress = 0;
                    } else {
                        stress = (Float.parseFloat(heartRateInfo.getHeartRates().get(i).averageHeartRate) - 60);
                    }
                    barEntries.add(new BarEntry(i, stress));
                    theDates.add(heartRateInfo.getHeartRates().get(i).start);
                }

                BarDataSet barDataSet = new BarDataSet(barEntries, "Stress level");

                BarData theDate = new BarData(barDataSet);
                barChart.setData(theDate);

            }
        }
    };


}