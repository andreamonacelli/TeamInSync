package com.monacdev.teaminsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.utils.Constants;

public class MainActivity extends AppCompatActivity {
    private String loggedUserUsername;
    private String loggedUserName;
    private ImageView teamLogoIV;
    private TextView homepageHeaderTV;
    private Button toTrainingPageBtn;
    private Button squadListBtn;
    private TextView teamNameTV;
    private TextView leagueNameTV;
    private TextView cityStadiumTV;
    private ImageButton openNotificationsBtn;
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

        /* Defining Button Listeners */
        this.squadListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent squadListIntent = new Intent(MainActivity.this, MembersListActivity.class);
                startActivity(squadListIntent);
            }
        });
        this.toTrainingPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent trainingPageIntent = new Intent(MainActivity.this, TrainingActivity.class);
                startActivity(trainingPageIntent);
            }
        });
        this.openNotificationsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                * TODO: implement a Fragment to show notifications
                */
            }
        });
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
                    homepageHeaderTV.setText(String.format("%s %s", getResources().getText(R.string.homepage_welcome), loggedUserName));
                    String teamID = snapshot.child(Constants.TEAM_KEY_STRING).getValue(String.class);
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
}