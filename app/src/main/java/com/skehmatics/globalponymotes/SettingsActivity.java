package com.skehmatics.globalponymotes;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import java.util.Random;


public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    IInAppBillingService inAppBillingService;
    ServiceConnection inAppBillingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            inAppBillingService = IInAppBillingService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            inAppBillingService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.settings);

//        SwitchPreference shortcut = (SwitchPreference) findPreference("shortcutEnabled");
//        ((CheckBoxPreference) findPreference("altShortcutMethod")).setEnabled(shortcut.isChecked());

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, inAppBillingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (inAppBillingService != null){
            unbindService(inAppBillingServiceConnection);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//Todo: Find alternate shortcut method
//        if (key.equals("shortcutEnabled")) {
//            SwitchPreference shortcut = (SwitchPreference) findPreference("shortcutEnabled");
//            CheckBoxPreference altMethod = (CheckBoxPreference) findPreference("altShortcutMethod");
//            altMethod.setEnabled(shortcut.isChecked());
//            if (!altMethod.isEnabled()) {
//                altMethod.setChecked(false);
//            }
//        }
        if (key.equals("customPath")) {
            EditTextPreference path = (EditTextPreference) findPreference(key);
            String text = path.getText();
            if (text.endsWith("/")){
                path.setText(text.subSequence(0, text.length()-1).toString());
            }
        }

        if (key.equals("donationKeyYay")) {

            ListPreference donateSelector = (ListPreference) findPreference(key);
            String value = donateSelector.getValue();

            if (value == null) {
                return;
            } else {
                donateSelector.setValue(null);
            }

            if (value.equals("hug")) {
                String[] hugs = getResources().getStringArray(R.array.hugs);
                String hugEmote = hugs[new Random().nextInt(hugs.length)];
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:skehmatics@gmail.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, hugEmote);
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.donate_hug_text));
                startActivity(intent);
                return;
            }

            try {
                int result = inAppBillingService.isBillingSupported(3, getPackageName(), "inapp");
                if (result == 0) {

                    Bundle donateBundle = inAppBillingService.getBuyIntent(3, getPackageName(), value, "inapp", "");
                    if (donateBundle.getInt("RESPONSE_CODE") != 0) {
                        throw new IntentSender.SendIntentException();
                    }
                    PendingIntent donateIntent = donateBundle.getParcelable("BUY_INTENT");
                    startIntentSenderForResult(donateIntent.getIntentSender(), 100, new Intent(), 0, 0, 0);

                } else {
                    int resultId;
                    switch (result) {
                        case 3:
                            resultId = R.string.toast_error_billing_allowed;
                        case 7:
                            resultId = R.string.toast_error_billing_owned;
                        default:
                            resultId = R.string.toast_error_billing_other;
                    }
                    Toast.makeText(this, resultId, Toast.LENGTH_SHORT).show();

                }

            } catch (RemoteException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.toast_error_billing_connection, Toast.LENGTH_LONG).show();

            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(this, R.string.reboot_required, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case 100:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, R.string.toast_donation_success, Toast.LENGTH_SHORT).show();
                }
        }
    }
}
