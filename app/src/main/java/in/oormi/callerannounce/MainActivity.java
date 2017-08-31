package in.oormi.callerannounce;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static String[] PERMISSIONS_LIST = {Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE};
    private static final int REQUEST_CODE = 112;
    TextToSpeech tts;
    String username;
    boolean greetenable;
    String gstart;
    String gstop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermissions();
        Setup();

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            Intent i = new Intent(MainActivity.this, PhoneCallStatesService.class);

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final ImageView ring = (ImageView) findViewById(R.id.imageRad);
                if (isChecked) {
                    startService(i);
                    final Animation radAnimation = AnimationUtils.loadAnimation(MainActivity.this,
                            R.anim.radiate);
                    ring.startAnimation(radAnimation);
                    greet(1);
                } else {
                    stopService(i);
                    ring.clearAnimation();
                    greet(2);
                }
            }
        });

        ImageButton mbuttonSetting = (ImageButton) findViewById(R.id.imageButtonSettings);
        mbuttonSetting.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View view){
                        greetenable = false;
                        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
                        if (toggle.isChecked()) toggle.toggle();//find a better solution
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    }
                });

        ImageButton mbuttonRes = (ImageButton) findViewById(R.id.imageButtonInfo);
        mbuttonRes.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View view){
                        greetenable = false;
                        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
                        if (toggle.isChecked()) toggle.toggle();
                        Intent intent = new Intent(MainActivity.this, ResourceShow.class);
                        startActivity(intent);
                    }
                });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean contactsAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean phoneAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (contactsAccepted && phoneAccepted) {
                        Toast.makeText(MainActivity.this,
                                "Permission Granted for contacts and phone", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(MainActivity.this,
                                "Permission Denied, the app may not work. Try again.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        }
    }

    public void getPermissions(){
        int result1 = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_CONTACTS);
        int result2 = ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE);
        boolean result = result1 == PackageManager.PERMISSION_GRANTED
                && result2 == PackageManager.PERMISSION_GRANTED;

        if (result) {
            Toast.makeText(MainActivity.this, "Permissions are OK", Toast.LENGTH_LONG).show();
        } else {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setCancelable(false);
            dlgAlert.setMessage("This app needs permissions to announce caller numbers and names.\n\n" +
                    "This needs to be done only once. You can revoke permissions from App Settings.\n\n" +
                    "The app will close automatically if it fails to get permissions for some reason.");
            dlgAlert.setTitle("Permissions");
            dlgAlert.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    PERMISSIONS_LIST, REQUEST_CODE);
                        }
                    });
            dlgAlert.create().show();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                moveTaskToBack(true);
                return true;
        }
        return false;
    }

    public void speakString(final String str) {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.UK);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS Error", "This language is not supported");
                    } else {
                        tts.speak(str, TextToSpeech.QUEUE_FLUSH, null,
                                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                    }
                } else
                    Log.e("TTS Error", "Initialization Failed!");
            }
        });
    }

    private void Setup() {
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_headers, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        greetenable = prefs.getBoolean("greet_switch", true);
        username = prefs.getString("name_text", "");
        gstart = prefs.getString("gstart_list", "Caller Announce is active.");
        gstop = prefs.getString("gstop_list", "Caller Announce deactivated.");

    }

    public void greet(int type){
        if(greetenable){
            String gstr = "";
            if(type==1) gstr = username + "," + gstart;
            if(type==2) gstr = gstop;
            speakString(gstr);
        }
    }
}
