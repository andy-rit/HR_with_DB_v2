package com.rittie.andy.testing;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import com.microsoft.band.BandException;

public class RealMonitoringActivity extends AppCompatActivity {

    private User u;
    private TextView tvHeartRate;
    private TextView tvStatus;
    private Button btnConnect;
    private boolean isConnect = false;
    private BandClient client = null;
    private double[] hr;
    private double[] smoothed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_base_hr);
        Intent intent = getIntent();

        u = (User) intent.getParcelableExtra("user");
        smoothed = new double[] {0,0,0,0,0};

        tvStatus = (TextView) findViewById(R.id.xtvStatus);
        btnConnect = (Button) findViewById(R.id.xbtnStart);
        tvHeartRate = (TextView) findViewById(R.id.xtvHeartRate);
        btnConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //TODO Auto-generated method stub
                if (isConnect == false) {
                    new ListenerTask().execute();
                    btnConnect.setText("Exit");
                } else {
                    finish();
                }
            }
        });
    }

    private HeartRateConsentListener heartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {
        }
    };

    private BandHeartRateEventListener heartRateListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null)
                appendToUI(Integer.toString(event.getHeartRate()), 2);
            else finish();

        }
    };

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.", 1);
                tvStatus.setTextColor(Color.parseColor("#d04545"));
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        } else if(ConnectionState.UNBOUND == client.getConnectionState())
            return false;

        appendToUI("Band is connecting...", 1);
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private void unRegisterListeners(){
        try {
            client.getSensorManager().unregisterAllListeners();
        } catch (BandIOException e) {
            appendToUI(e.getMessage(), 1);
        }
    }

    private void appendToUI(final String string, final int code) {
        // code : 1 = status, 2 = hr, 3 = step, 4 = distance, 5 = speed, 6 = temperature


        this.runOnUiThread(new Runnable() {

            double arousalLevelStep = 0.01875;
            double currentAverage;
            double averageHeartRate = u.getAvgHR();
            double currentSum = 0;

            /*public void run() {
                double hr = dummyHR[i];
                tvActual.setText(String.format("%.2f",hr));
                for (int x = 0; x < pastResults.length - 1; x++) {
                    pastResults[x] = pastResults[x + 1];
                }
                pastResults[pastResults.length - 1] = hr;
                currentSum = 0;
                for (int x = 0; x < pastResults.length; x++) {
                    currentSum = currentSum + pastResults[x];
                }
                currentAverage = currentSum / pastResults.length;*/

            @Override
            public void run() {
                if(code == 1){
                    tvStatus.setText(string);
                }
                else if(code == 2) {

                    averageHeartRate = u.getAvgHR();

                    for (int x = 0; x < smoothed.length - 1; x++) {
                        smoothed[x] = smoothed[x + 1];
                    }
                    smoothed[smoothed.length - 1] = Double.parseDouble(string);
                    currentSum = 0;
                    for (int x = 0; x < smoothed.length; x++) {
                        currentSum = currentSum + smoothed[x];
                    }
                    currentAverage = currentSum / smoothed.length;

                    if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 9)) { //Level 10
                        tvHeartRate.setBackgroundColor(Color.RED);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 8)) { //Level 9
                        tvHeartRate.setBackgroundColor(Color.RED);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 7)) { //Level 8
                        tvHeartRate.setBackgroundColor(Color.RED);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 6)) { //Level 7
                        tvHeartRate.setBackgroundColor(Color.RED);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 5)) { //Level 6
                        tvHeartRate.setBackgroundColor(Color.YELLOW);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 4)) { //Level 5
                        tvHeartRate.setBackgroundColor(Color.CYAN);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 3)) { //Level 4
                        tvHeartRate.setBackgroundColor(Color.CYAN);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep * 2)) { //Level 3
                        tvHeartRate.setBackgroundColor(Color.MAGENTA);
                    } else if (currentAverage > averageHeartRate * (1 + arousalLevelStep)) { //Level 2
                        tvHeartRate.setBackgroundColor(Color.BLUE);
                    } else if (currentAverage > averageHeartRate) { //Level 1
                        tvHeartRate.setBackgroundColor(Color.GREEN);
                    } else { //Level 0
                        tvHeartRate.setBackgroundColor(Color.GREEN);
                    }

                    tvHeartRate.setText(String.valueOf(currentAverage)+" BPM");
                }
            }

        });
    }

    // execute thread di asynctask

    private class ListenerTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.", 1);
                    isConnect = true;
                    if(client.getSensorManager().getCurrentHeartRateConsent() !=
                            UserConsent.GRANTED) {
                        client.getSensorManager().requestHeartRateConsent(RealMonitoringActivity.this, heartRateConsentListener);
                    }

                    client.getSensorManager().registerHeartRateEventListener(heartRateListener);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.", 1);
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                        break;
                    default:
                        exceptionMessage = e.getMessage();
                        break;
                }
                appendToUI(e.getMessage() + "\nAccept permision of Microsoft Health Service, then restart counting", 1);

            } catch (Exception e) {
                appendToUI(e.getMessage(), 1);
            }

            return null;
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unRegisterListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_real_monitoring, menu);
        return true;
    }
    @Override
    public void onBackPressed(){
        Intent in = new Intent(this, UserHomeActivity.class);
        in.putExtra("user", u);
        startActivity(in);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}