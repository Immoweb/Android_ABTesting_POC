package be.ibad.abfirebase;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private static final String LOADING_PHRASE_CONFIG_KEY = "loading_phrase";
    private static final String WELCOME_MESSAGE_KEY = "welcome_message";
    private static final String WELCOME_MESSAGE_CAPS_KEY = "welcome_message_caps";

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private TextView mTextMessage;
    private Button mButton;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            String screenName = null;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    screenName = getString(R.string.title_home);
                    break;
                case R.id.navigation_dashboard:
                    screenName = getString(R.string.title_dashboard);
                    break;
                case R.id.navigation_notifications:
                    screenName = getString(R.string.title_notifications);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("You want a Crash ?")
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            int i = 5 / 0;
                                        }
                                    }).show();
                    break;
            }
            FirebaseAnalytics.getInstance(MainActivity.this).setCurrentScreen(MainActivity.this, screenName, null);
            getSupportActionBar().setTitle(screenName);
            return true;
        }
    };
    private ImageView circleImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                int buttonColor = button.getBackgroundTintList().getDefaultColor();
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_VARIANT, String.valueOf(button.getText()));
                bundle.putString("item_color", buttonColor == R.color.colorPrimary ? "ColorPrimary" : "Green");
                FirebaseAnalytics.getInstance(MainActivity.this).logEvent("CheckoutClicked", bundle);

                FirebaseCrash.report(new Exception("My first Android non-fatal error"));
            }
        });

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        fetchWelcome();
        circleImageView = (ImageView) findViewById(R.id.circle_img_view);

    }

    /**
     * Fetch a welcome message from the Remote Config service, and then activate it.
     */
    private void fetchWelcome() {
        mTextMessage.setText(mFirebaseRemoteConfig.getString(LOADING_PHRASE_CONFIG_KEY));

        long cacheExpiration = 3600; // 1 hour in seconds.
        // If your app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        // [START fetch_config_with_callback]
        // cacheExpirationSeconds is set to cacheExpiration here, indicating the next fetch request
        // will use fetch data from the Remote Config service, rather than cached parameter values,
        // if cached parameter values are more than cacheExpiration seconds old.
        // See Best Practices in the README for more information.
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Fetch Succeeded",
                                    Toast.LENGTH_SHORT).show();

                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            Toast.makeText(MainActivity.this, "Fetch Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                        displayWelcomeMessage();
                        displayTextExperiment();
                        displayButtonExperiment();
                        displayFetchedImage();
                    }
                });
        // [END fetch_config_with_callback]
    }

    private void displayFetchedImage() {
        String imageUrl = mFirebaseRemoteConfig.getString("image_url");
        final Trace myTrace = FirebasePerformance.getInstance().newTrace("test_trace");
        myTrace.start();
        Picasso.with(MainActivity.this)
                .load(imageUrl)
                .into(circleImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        myTrace.stop();
                    }

                    @Override
                    public void onError() {
                        myTrace.stop();
                    }
                });
    }

    private void displayButtonExperiment() {
        String experiment = mFirebaseRemoteConfig.getString("experiment_variant_button");
        FirebaseAnalytics.getInstance(this).setUserProperty("Experiment_Button", experiment);
        if (experiment.equals("variant_c")) {
            mButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green)));
            mButton.setTextColor(Color.WHITE);
            mButton.setText("Buy");
        } else if (experiment.equals("variant_d")) {
            mButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
            mButton.setTextColor(Color.WHITE);
            mButton.setText("Checkout");
        }
    }

    private void displayTextExperiment() {
        String experiment = mFirebaseRemoteConfig.getString("experiment_variant_text");
        FirebaseAnalytics.getInstance(this).setUserProperty("Experiment_Text", experiment);
        if (experiment.equals("variant_a")) {
            mTextMessage.setTextColor(Color.parseColor("#303F9F"));
        } else if (experiment.equals("variant_b")) {
            mTextMessage.setTextColor(Color.parseColor("#388E3C"));
        }
    }

    private void displayWelcomeMessage() {
        // [START get_config_values]
        String welcomeMessage = mFirebaseRemoteConfig.getString(WELCOME_MESSAGE_KEY);
        // [END get_config_values]
        if (mFirebaseRemoteConfig.getBoolean(WELCOME_MESSAGE_CAPS_KEY)) {
            mTextMessage.setAllCaps(true);
        } else {
            mTextMessage.setAllCaps(false);
        }
        mTextMessage.setText(welcomeMessage);
    }
}
