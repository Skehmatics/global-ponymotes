package com.skehmatics.globalponymotes;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    static int READ_PERMISSION_REQCODE = 42;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView emote = (TextView) findViewById(R.id.emoteEmote);
        TextView feedback = (TextView) findViewById(R.id.emoteFeedback);
        Button bugButton = (Button) findViewById(R.id.bugButton);
        LinearLayout layout = (LinearLayout) findViewById(R.id.extraLayout);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String customPath = prefs.getString("customPath", "");
        String path = customPath.trim().isEmpty() ? Environment.getExternalStorageDirectory().toString() + "/RedditEmotes" : customPath;

        Drawable testDrawable = Drawable.createFromPath(path + "/adorkable.png");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_REQCODE);
            emote.setText("Hi there!");
            feedback.setText("I need access to your phone storage to find emotes.");

            bugButton.setText("REQUEST");
            bugButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    recreate();
                }
            });
        } else {

            if (isXposedEnabled() && testDrawable != null) {
                emote.setText(R.string.status_pass);
                feedback.setText(R.string.feedback_pass);
                layout.setVisibility(View.VISIBLE);
                bugButton.setVisibility(View.GONE);
            } else {
                emote.setText(R.string.status_fail);
                layout.setEnabled(false);

                bugButton.setText("REPORT BUG");
                bugButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:skehmatics@gmail.com"));
                        intent.putExtra(Intent.EXTRA_SUBJECT, "GPM BUG: ");
                        intent.putExtra(Intent.EXTRA_TEXT, "Describe your the issue here. Make sure to include information like your device and Android version.");
                        startActivity(intent);
                    }
                });

                if (testDrawable == null) {
                    feedback.setText(R.string.feedback_fail_io);
                    feedback.append("\n\nPath: " + path);
                } else {
                    feedback.setText(R.string.feedback_fail_xposed);
                }
            }

        }
    }

    public void doToast(View v){
        Toast.makeText(this, Html.fromHtml(getString(R.string.temporary_workaround)), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_menu_settings:
                toSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toSettings(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public static boolean isXposedEnabled(){
        return false;
    }
}