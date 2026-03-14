package com.monacdev.teaminsync.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.utils.Constants;
import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity {
    private String teamID;
    private String loggedUserUsername;
    private String loggedUserName;
    private String loggedUserSurname;
    private ImageView teamLogoIV;
    private TextView homepageHeaderTV;
    private Button toTrainingPageBtn;
    private Button squadListBtn;
    private TextView teamNameTV;
    private TextView leagueNameTV;
    private TextView cityStadiumTV;
    private ImageButton openNotificationsBtn;
    private ImageButton logoutBtn;
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.bindViewsWithObjects();
        this.populateDataFromDB();
        this.setListeners();
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     */
    private void bindViewsWithObjects(){
        this.teamLogoIV = findViewById(R.id.teamLogoIV);
        this.homepageHeaderTV = findViewById(R.id.homepageHeaderTV);
        this.toTrainingPageBtn = findViewById(R.id.toTrainingPageBtn);
        this.squadListBtn = findViewById(R.id.squadListBtn);
        this.teamNameTV = findViewById(R.id.teamNameTV);
        this.leagueNameTV = findViewById(R.id.leagueNameTV);
        this.cityStadiumTV = findViewById(R.id.cityStadiumTV);
        this.openNotificationsBtn = findViewById(R.id.openNotificationsBtn);
        this.logoutBtn = findViewById(R.id.logoutBtn);
    }

    /**
     * Defines the listeners for the View components within the current activity
     */
    private void setListeners(){
        this.squadListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent squadListIntent = new Intent(MainActivity.this, MembersListActivity.class);
                squadListIntent.putExtra(Constants.TEAM_ID_TAG, teamID);
                startActivity(squadListIntent);
            }
        });
        this.toTrainingPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent trainingPageIntent = new Intent(MainActivity.this, TrainingActivity.class);
                trainingPageIntent.putExtra(Constants.DISPLAYED_USER_EXTRA_STRING, loggedUserUsername);
                trainingPageIntent.putExtra(Constants.DISPLAYED_USER_SURNAME_EXTRA_STRING, loggedUserSurname);
                startActivity(trainingPageIntent);
            }
        });
        this.openNotificationsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* TODO: implement a Fragment to show notifications */
            }
        });
        this.logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.handleLogoutConfirmation();
            }
        });
    }

    /**
     * Based on the logged user, fetches data from the DB and shows the respective team data
     */
    private void populateDataFromDB(){
        Intent callerIntent = this.getIntent();
        this.loggedUserUsername = callerIntent.getStringExtra(Constants.LOGGED_USER_EXTRA_STRING);
        DatabaseReference userRef = this.dbRef.child(Constants.USERS_REFERENCE_STRING).child(this.loggedUserUsername);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    loggedUserName = snapshot.child(Constants.NAME_KEY_STRING).getValue(String.class);
                    loggedUserSurname = snapshot.child(Constants.SURNAME_KEY_STRING).getValue(String.class);
                    homepageHeaderTV.setText(String.format("%s %s", getResources().getText(R.string.homepage_welcome), loggedUserName));
                    teamID = snapshot.child(Constants.TEAM_KEY_STRING).getValue(String.class);
                    if(teamID != null && !teamID.isEmpty()){
                        fetchTeamData(teamID);
                    }
                    /* TODO: handle the pending request scenario (workflow yet to be clearly defined) */
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Given the ID of the team to which the user belongs, fetch the data related to the team itself
     * @param teamID the ID of the team to which the logged user belongs
     */
    private void fetchTeamData(String teamID){
        DatabaseReference teamRef = this.dbRef.child(Constants.TEAMS_REFERENCE_STRING).child(teamID);
        teamRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String teamName = snapshot.child(Constants.NAME_KEY_STRING).getValue(String.class);
                    String leagueName = snapshot.child(Constants.LEAGUE_KEY_STRING).getValue(String.class);
                    String address = snapshot.child(Constants.ADDRESS_KEY_STRING).getValue(String.class);
                    String stadium = snapshot.child(Constants.STADIUM_KEY_STRING).getValue(String.class);
                    String logoPath = snapshot.child(Constants.LOGO_KEY_STRING).getValue(String.class);
                    if(teamName != null){
                        teamNameTV.setText(teamName);
                    }
                    if(leagueName != null){
                        leagueNameTV.setText(leagueName);
                    }
                    if(address != null && stadium != null){
                        cityStadiumTV.setText(String.format("%s - %s", stadium, address));
                    }
                    if(logoPath != null && !logoPath.isEmpty()){
                        Glide.with(MainActivity.this).load(logoPath).circleCrop().into(teamLogoIV);
                    } else {
                        teamLogoIV.setImageResource(R.mipmap.ic_launcher);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Listens for incoming notifications on the Firebase DB
     */
    private void listenForNewNotifications(){
        DatabaseReference notificationsRef = this.dbRef.child(Constants.NOTIFICATIONS_REFERENCE_STRING).child(this.loggedUserUsername);
        notificationsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                /* New notification arrived on the DB */

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Creates and sets up the notification channel in order to implement the local-push mechanism for incoming notifications
     */
    private void createNotificationChannel() {

    }

    /**
     * Logs out the currently authenticated user and cleans the persistent SharedPreferences for the application
     */
    private void logoutUser(){
        /* Cleaning the SharedPreferences used for persistence */
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_STRING, MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
        sharedPrefsEditor.remove(Constants.LOGGED_USER_EXTRA_STRING);
        sharedPrefsEditor.apply();
        /* Effectively Sign Out */
        FirebaseAuth.getInstance().signOut();
        OneSignal.logout();
        /* Delay activity destruction to allow OneSignal to complete logout */
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                /* Rerouting to the Login Activity */
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }
        }, 500);
    }

    /**
     * Shows a dialog in order to handle logout confirmation before initiating the logout procedure itself
     */
    private void handleLogoutConfirmation(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(R.string.confirm_logout);
        dialogBuilder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.logoutUser();
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.i(Constants.LOGOUT_CANCELED_TAG, Constants.LOGOUT_CANCELED_MSG);
            }
        });
        AlertDialog logoutDialog = dialogBuilder.create();
        logoutDialog.show();
    }
}