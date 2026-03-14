package com.monacdev.teaminsync.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.monacdev.teaminsync.utils.Constants;
import com.monacdev.teaminsync.utils.PushNotificationsManager;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.initializeNotificationsSystem();
        this.defineNextRoute();
    }

    /**
     * Based on whether there is a user logged or not, this defines the correct user route between the MainActivity and the LoginActivity
     */
    private void defineNextRoute(){
        FirebaseUser loggedUser = FirebaseAuth.getInstance().getCurrentUser();
        if(loggedUser != null){
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_STRING, MODE_PRIVATE);
            String loggedUsername = sharedPreferences.getString(Constants.LOGGED_USER_EXTRA_STRING, null);
            if(loggedUsername != null){
                Intent homepageIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                homepageIntent.putExtra(Constants.LOGGED_USER_EXTRA_STRING, loggedUsername);
                OneSignal.login(loggedUsername);
                startActivity(homepageIntent);
                finish();
                return;
            }
        }
        /* If we get to this part of the code it means that for some reason no user was logged beforehand */
        Intent loginIntent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    /**
     * Initializes the notification system based on OneSignal
     */
    private void initializeNotificationsSystem(){
        OneSignal.getDebug().setAlertLevel(LogLevel.FATAL);
        OneSignal.initWithContext(SplashScreenActivity.this, PushNotificationsManager.ONESIGNAL_APP_ID);
        OneSignal.getNotifications().requestPermission(true, Continue.with(r -> {
            /* Do nothing */
        }));
    }
}