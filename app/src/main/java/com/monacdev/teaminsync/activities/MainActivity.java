package com.monacdev.teaminsync.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.fragments.NotificationsFragment;
import com.monacdev.teaminsync.constants.Constants;
import com.monacdev.teaminsync.loaders.LoaderDialog;
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
    private LoaderDialog loaderDialog;

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

        this.loaderDialog = new LoaderDialog(this);
        this.bindViewsWithObjects();
        this.populateDataFromDB();
        this.setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* This allows to reflect eventual changes in the user data */
        this.populateDataFromDB();
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
        this.squadListBtn.setOnClickListener(view -> {
            Intent squadListIntent = new Intent(MainActivity.this, MembersListActivity.class);
            squadListIntent.putExtra(IntentExtrasTags.TEAM_ID, teamID);
            startActivity(squadListIntent);
        });
        this.toTrainingPageBtn.setOnClickListener(view -> {
            Intent trainingPageIntent = new Intent(MainActivity.this, TrainingActivity.class);
            trainingPageIntent.putExtra(IntentExtrasTags.DISPLAYED_USER, loggedUserUsername);
            trainingPageIntent.putExtra(IntentExtrasTags.DISPLAYED_USER_SURNAME, loggedUserSurname);
            startActivity(trainingPageIntent);
        });
        this.openNotificationsBtn.setOnClickListener(view -> {
            NotificationsFragment notificationsDialog = NotificationsFragment.newInstance(loggedUserUsername);
            notificationsDialog.show(getSupportFragmentManager(), NavigationTags.NOTIFICATIONS_FRAGMENT);
        });
        this.logoutBtn.setOnClickListener(view -> MainActivity.this.handleLogoutConfirmation());
    }

    /**
     * Based on the logged user, fetches data from the DB and shows the respective team data
     */
    private void populateDataFromDB(){
        Intent callerIntent = this.getIntent();
        this.loggedUserUsername = callerIntent.getStringExtra(IntentExtrasTags.LOGGED_USER);
        DatabaseReference userRef = this.dbRef.child(ReferenceStrings.USERS).child(this.loggedUserUsername);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    loggedUserName = snapshot.child(KeyStrings.NAME).getValue(String.class);
                    loggedUserSurname = snapshot.child(KeyStrings.SURNAME).getValue(String.class);
                    homepageHeaderTV.setText(String.format("%s %s", getResources().getText(R.string.homepage_welcome), loggedUserName));
                    teamID = snapshot.child(KeyStrings.TEAM).getValue(String.class);
                    if(teamID != null && !teamID.isEmpty()){
                        fetchTeamData(teamID);
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
     * Given the ID of the team to which the user belongs, fetch the data related to the team itself
     * @param teamID the ID of the team to which the logged user belongs
     */
    private void fetchTeamData(String teamID){
        DatabaseReference teamRef = this.dbRef.child(ReferenceStrings.TEAMS).child(teamID);
        teamRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String teamName = snapshot.child(KeyStrings.NAME).getValue(String.class);
                    String leagueName = snapshot.child(KeyStrings.LEAGUE).getValue(String.class);
                    String address = snapshot.child(KeyStrings.ADDRESS).getValue(String.class);
                    String stadium = snapshot.child(KeyStrings.STADIUM).getValue(String.class);
                    String logoPath = snapshot.child(KeyStrings.LOGO).getValue(String.class);
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
     * Logs out the currently authenticated user and cleans the persistent SharedPreferences for the application
     */
    private void logoutUser(){
        this.loaderDialog.show(getString(R.string.logout_load_msg));
        /* Cleaning the SharedPreferences used for persistence */
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_STRING, MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
        sharedPrefsEditor.remove(IntentExtrasTags.LOGGED_USER);
        sharedPrefsEditor.apply();
        /* Effectively Sign Out */
        FirebaseAuth.getInstance().signOut();
        OneSignal.logout();
        /* Delay activity destruction to allow OneSignal to complete logout */
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            /* Rerouting to the Login Activity while contextually clearing the previous activity stack */
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            this.loaderDialog.hide();
            startActivity(loginIntent);
            finish();
        }, 500);
    }

    /**
     * Shows a dialog in order to handle logout confirmation before initiating the logout procedure itself
     */
    private void handleLogoutConfirmation(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(R.string.confirm_logout);
        dialogBuilder.setPositiveButton(R.string.confirm, (dialogInterface, i) -> MainActivity.this.logoutUser());
        dialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> Log.i(NavigationTags.LOGOUT_CANCELED, Constants.LOGOUT_CANCELED_MSG));
        AlertDialog logoutDialog = dialogBuilder.create();
        logoutDialog.show();
    }
}