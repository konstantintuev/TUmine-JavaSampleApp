package tuev.co.mineinbkg;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

import tuev.co.tumine.InfoPassing;
import tuev.co.tumine.MineConnector;
import tuev.co.tumine.OnMessageReceived;
import tuev.co.tumine.OnRunningResult;
import tuev.co.tumine.OutputInfo;
import tuev.co.tumine.Pool;

public class MainActivity extends AppCompatActivity {

    //our views
    private EditText pool;
    private EditText username;
    private EditText password;
    private Button run;

    private TextView accepted;
    private TextView hashrate;

    //this is needed to control and communicate with the miner
    private InfoPassing infoPassing;
    private MineConnector mineConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        //BAREBONES

        final int halfTheCores = InfoPassing.getAvailableCores() / 2;
        final int cores = halfTheCores < 1 ? 1 : halfTheCores;


        //OPTIONAL

        checkIfMinerIsRunning();

        mineConnector = new MineConnector(new OnMessageReceived() {

            //the miner has sent us data
            @Override
            public void messageReceived(OutputInfo outputInfo) {
                Log.d(MainActivity.class.getSimpleName(), "messageReceived: "+outputInfo.getLastChangedValue());
                if (!run.getText().equals("Stop")) {
                    run.setText("Stop");
                    run.setEnabled(true);
                }
                if (outputInfo.getLastChangedValue() == null) {
                    return;
                }

                //get what has changed
                switch (outputInfo.getLastChangedValue()) {
                    case hashrate:
                        Log.d(MainActivity.class.getSimpleName(),outputInfo.getHashrate().getHighest() + "H/s");
                        hashrate.setText(String.format(Locale.ENGLISH,"Hashrate: %s H/s", outputInfo.getHashrate().getHighest()));
                        break;
                    case lastError:
                        Log.d(MainActivity.class.getSimpleName(), "Error: " + outputInfo.getLastError());
                        break;
                    case message:
                        Log.d(MainActivity.class.getSimpleName(), "Msg: " + outputInfo.getMessage());
                        break;
                    case initInfo:
                        Log.d(MainActivity.class.getSimpleName(), "InitInfo: pkgName: " + outputInfo.getInitInfo().getPkg()
                                + " royalty: " +outputInfo.getInitInfo().getPaid()
                                + " royalty percentage: " + outputInfo.getInitInfo().getPercentageToMe());
                        break;
                    case lastMiningJobResult:
                        Log.d(MainActivity.class.getSimpleName(), "MiningJobResult: accepted: " + outputInfo.getLastMiningJobResult().getAccepted() +
                                " rejected: "+outputInfo.getLastMiningJobResult().getRejected() +
                                " difficulty: " + outputInfo.getLastMiningJobResult().getDifficulty() +
                                " elapsed: "+outputInfo.getLastMiningJobResult().getError());
                        accepted.setText(String.format(Locale.ENGLISH, "Accepted: %d", outputInfo.getLastMiningJobResult().getAccepted()));
                        break;
                    case debugInfo:
                        Log.d(MainActivity.class.getSimpleName(), "DebugInfo: " + TextUtils.join("\n", outputInfo.getDebugInfo().getPools()));
                        break;
                    case lastMiningJob:
                        Log.d(MainActivity.class.getSimpleName(), "MiningJob: " + outputInfo.getLastMiningJob().getDifficulty());
                        break;
                    case startMiningInfo:
                        Log.d(MainActivity.class.getSimpleName(), "StartMiningInfo: " + outputInfo.getStartMiningInfo().getMemoryInMB());
                        break;

                }

            }

            //Connection to the miner has been established
            @Override
            public void connected() {
                Log.d(getClass().getSimpleName(),"Miner started, waiting for Hashrate...");
            }

            //when the current instance of the miner has stopped
            @Override
            public void stopped() {
                run.setText("Run");
                run.setEnabled(true);
                hashrate.setText(String.format(Locale.ENGLISH,"Hashrate: %s H/s", 0));
                accepted.setText(String.format(Locale.ENGLISH, "Accepted: %d", 0));
            }


            /**
             * @param b = isBasicLogging parse data from the miner ONLY about:
             *    - lastMiningJobResult
             *    - hashrate and hashratePerThread
             *    - lastError
             *    - initInfo
             *    - message
             */
        }, this, false, true);

        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (run.getText().equals("Run")) {
                    //                            pass context
                    infoPassing = new InfoPassing(MainActivity.this);
                    //add all the mining pools (the first is used, other are for backup)
                    infoPassing.getMinerConfig().getPools().add(new Pool(pool.getText().toString(), username.getText().toString(), password.getText().toString()).setUseSSL(true));
                    //which cores to use normally
                    infoPassing.getMinerConfig().setCoresToUse(cores);
                    //set class made to provide the service with a Notification
                    infoPassing.getMiningInAndroid().setNotificationGetterClass(ProvideNotification.class);
                    //download ssl supported binaries from my website
                    infoPassing.getMinerConfig().setUseSSL(true);
                    //acquire partial wakelock - keep the cpu alive while mining
                    infoPassing.getMiningInAndroid().setKeepCPUawake(true);
                    //no connection between the miner and the JAVA code
                    //this option makes the 'MineConnector' useless when enabled
                    infoPassing.getMinerOutput().setSilent(false);
                    //check for updates every time before starting to miner
                    infoPassing.getMinerConfig().setUpdateOverInternet(false);

                    infoPassing.getMinerOutput().setDebugParams(true);

                    infoPassing.getMinerOutput().setUseMinus11InsteadOfNull(true);

                    infoPassing.startMiningService();
                    run.setEnabled(false);
                    run.setText("Starting...");
                } else {
                    InfoPassing.stopMiningService(MainActivity.this);
                    run.setEnabled(false);
                    run.setText("Stopping...");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            checkIfMinerIsRunning();
                        }
                    }, 1000);
                }
            }
        });


    }

    private void checkIfMinerIsRunning() {
        InfoPassing.checkIfMinerIsRunning(new OnRunningResult() {
            @Override
            public void result(boolean b) {
                run.setEnabled(true);
                if (!b) {
                    run.setText("Run");
                    hashrate.setText(String.format(Locale.ENGLISH,"Hashrate: %s H/s", 0));
                    accepted.setText(String.format(Locale.ENGLISH, "Accepted: %d", 0));
                } else {
                    run.setText("Stop");
                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mineConnector.detach();
    }

    private void initViews() {
        pool = findViewById(R.id.pool);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        run = findViewById(R.id.run);

        hashrate = findViewById(R.id.hashrate);
        accepted = findViewById(R.id.accepted);
    }
}
