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

public class RealBaseHRActivity extends AppCompatActivity {

    private User u;
    private TextView tvHeartRate;
    private TextView tvStatus;
    private Button btnConnect;
    private boolean isConnect = false;
    private BandClient client = null;
    private double[] hr;
    private int i;
    DBAdapter db = new DBAdapter(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_base_hr);
        Intent intent = getIntent();

        u = (User) intent.getParcelableExtra("user");

        tvStatus = (TextView) findViewById(R.id.xtvStatus);
        btnConnect = (Button) findViewById(R.id.xbtnStart);
        tvHeartRate = (TextView) findViewById(R.id.xtvHeartRate);
        hr = new double[30];
        i= 0;
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
            if (event != null) {
                if (i <= hr.length - 1) {
                    appendToUI(Integer.toString(event.getHeartRate()), 2);
                }
                else {
                    db.open();
                    //change restingHR[i] to input from band
                    for (int x=0; x<hr.length; x++) {
                        db.insertHeartRate(String.valueOf(u.getId()), String.valueOf(hr[x]));
                    }
                    db.close();

                    appendToUI(String.valueOf(u.calcAvg(hr)), 3);

                    db.open();
                    boolean b = db.updateUserRecord(u.getId(),u.getName(),u.getEmail(),u.getPassword(),String.valueOf(u.getAvgHR()));
                    db.close();
                    //finish();
                }
            }
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
            @Override
            public void run() {
                if(code == 1){
                    tvStatus.setText(string);
                }
                else if(code == 2) {
                    hr[i] = Double.parseDouble(string);
                    tvHeartRate.setText(string+" BPM");
                    i++;
                }
                else if(code == 3) {
                    tvStatus.setText("Finished, average:");
                    tvHeartRate.setText(string+" BPM");
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
                        client.getSensorManager().requestHeartRateConsent(RealBaseHRActivity.this, heartRateConsentListener);
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
        getMenuInflater().inflate(R.menu.menu_real_base_hr, menu);
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