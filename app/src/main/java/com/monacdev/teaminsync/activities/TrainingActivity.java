package com.monacdev.teaminsync.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.TrainingListTileAdapter;
import com.monacdev.teaminsync.fragments.TrainingCreationWizardFragment;
import com.monacdev.teaminsync.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class TrainingActivity extends AppCompatActivity {
    private TextView trainingPageHeaderTV;
    private RecyclerView trainingListRV;
    private View trainingTrackerFragmentContainer;
    private ImageButton createTrainingBtn;
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private String displayedUserID;
    private String displayedUserSurname;
    private String viewedTeamID;
    private boolean isMyTrainingList;
    private String loggedUserRole; /* Actually useful only for buttons visibility's sake */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_training);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.displayedUserID = getIntent().getStringExtra(Constants.DISPLAYED_USER_EXTRA_STRING);
        this.displayedUserSurname = getIntent().getStringExtra(Constants.DISPLAYED_USER_SURNAME_EXTRA_STRING);
        this.bindViewsToObjects();
        this.setListeners();
        this.isDisplayedUserLogged();
        this.trainingPageHeaderTV.setText(String.format("%s %s", getString(R.string.training_list_header), this.displayedUserSurname));
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     */
    private void bindViewsToObjects(){
        this.trainingPageHeaderTV = findViewById(R.id.trainingPageHeaderTV);
        this.trainingListRV = findViewById(R.id.trainingListRV);
        this.trainingListRV.setLayoutManager(new LinearLayoutManager(this));
        this.createTrainingBtn = findViewById(R.id.createTrainingBtn);
        this.trainingTrackerFragmentContainer = findViewById(R.id.trainingTrackerFragmentContainer);
    }

    /**
     * Checks if the current training page corresponds to the one of the currently logged user and adjusts the UI accordingly
     */
    private void isDisplayedUserLogged(){
        FirebaseAuth authClient = FirebaseAuth.getInstance();
        FirebaseUser loggedUser = authClient.getCurrentUser();
        if(loggedUser != null){
            String loggedUserEmail = loggedUser.getEmail();
            DatabaseReference displayedUserRef = this.dbRef.child(Constants.USERS_REFERENCE_STRING).child(this.displayedUserID);
            displayedUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String displayedUserRole = null;
                    if(snapshot.exists()){
                        viewedTeamID = snapshot.child(Constants.TEAM_KEY_STRING).getValue(String.class);
                        String displayedUserEmail = snapshot.child(Constants.EMAIL_KEY_STRING).getValue(String.class);
                        displayedUserRole = snapshot.child(Constants.ROLE_KEY_STRING).getValue(String.class);
                        if(loggedUserEmail != null && loggedUserEmail.equals(displayedUserEmail)){
                            isMyTrainingList = true;
                            setAddTrainingBtnVisibility(displayedUserRole);
                        } else {
                            isMyTrainingList = false;
                        }
                    } else {
                        isMyTrainingList = false;
                    }
                    String roleForFetch = displayedUserRole != null ? displayedUserRole : Constants.PLAYER_ROLE_STRING;
                    TrainingActivity.this.fetchTrainingsFromDB(TrainingActivity.this.displayedUserID, roleForFetch);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TrainingActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
                    isMyTrainingList = false;
                }
            });
        } else {
            isMyTrainingList = false;
        }
    }

    /**
     * If the displayed user is the currently logged one and the user is a coach then display the "Add Training Button"
     */
    private void setAddTrainingBtnVisibility(String role){
        if(this.isMyTrainingList){
            this.loggedUserRole = role;
            if(role != null && role.equals(Constants.COACH_ROLE_STRING)){
                this.createTrainingBtn.setVisibility(View.VISIBLE);
            } else {
                this.createTrainingBtn.setVisibility(View.GONE);
            }
        } else {
            this.createTrainingBtn.setVisibility(View.GONE);
        }
    }

    /**
     * Defines the listeners for the View components within the current activity
     */
    private void setListeners(){
        this.createTrainingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrainingCreationWizardFragment trainingCreationWizard = TrainingCreationWizardFragment.newInstance(TrainingActivity.this.viewedTeamID, TrainingActivity.this.displayedUserID);
                trainingCreationWizard.show(getSupportFragmentManager(), Constants.TRAINING_CREATION_WIZARD_TAG);
            }
        });
    }

    /**
     * Fetches the list of trainings for the displayed user from the Firebase DB
     * @param displayedUserID the user whose trainings are going to be displayed
     * @param role the role of the currently logged user
     */
    private void fetchTrainingsFromDB(String displayedUserID, String role){
        ArrayList<HashMap<String, String>> exercisesList = new ArrayList<>();
        Query q;
        if(role != null && role.equals(Constants.PLAYER_ROLE_STRING)){
            /* Fetching workflow in case the displayed user is a coach */
            q = this.dbRef.child(Constants.TRAININGS_REFERENCE_STRING).child(displayedUserID);
            q.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    exercisesList.clear();
                    if(snapshot.exists()){
                        for(DataSnapshot trainingSnapshot : snapshot.getChildren()){
                            HashMap<String, String> training = parseTrainingDataFromSnapshot(trainingSnapshot);
                            exercisesList.add(training);
                        }
                        TrainingListTileAdapter trainingsAdapter = new TrainingListTileAdapter(exercisesList, TrainingActivity.this.isMyTrainingList);
                        TrainingActivity.this.trainingListRV.setAdapter(trainingsAdapter);
                    } else {
                        Toast.makeText(TrainingActivity.this, R.string.no_trainings_found, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TrainingActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            /* Fetching workflow in case the displayed user is a coach */
            q = this.dbRef.child(Constants.TRAININGS_REFERENCE_STRING);
            q.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    exercisesList.clear();
                    ArrayList<String> trainingUIDs = new ArrayList<>();
                    if(snapshot.exists()){
                        for(DataSnapshot athleteTrainingSnapshot : snapshot.getChildren()){
                            for(DataSnapshot trainingSnapshot : athleteTrainingSnapshot.getChildren()){
                                String coachID = trainingSnapshot.child(Constants.COACH_ROLE_STRING).getValue(String.class);
                                String exerciseUID = trainingSnapshot.getKey();
                                if(coachID != null && coachID.equals(displayedUserID) && !trainingUIDs.contains(exerciseUID)){
                                    trainingUIDs.add(exerciseUID);
                                    HashMap<String, String> training = parseTrainingDataFromSnapshot(trainingSnapshot);
                                    exercisesList.add(training);
                                }
                            }
                        }
                        TrainingListTileAdapter trainingsAdapter = new TrainingListTileAdapter(exercisesList, TrainingActivity.this.isMyTrainingList);
                        TrainingActivity.this.trainingListRV.setAdapter(trainingsAdapter);
                    } else {
                        Toast.makeText(TrainingActivity.this, R.string.no_trainings_found, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TrainingActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given a DataSnapshot representing a training exercise, parses the respective data into a HashMap
     * @param trainingSnapshot the DataSnapshot from which the data needs to be parsed
     * @return a HashMap containing the parsed data
     */
    private HashMap<String, String> parseTrainingDataFromSnapshot(DataSnapshot trainingSnapshot){
        HashMap<String, String> training = new HashMap<>();
        training.put(Constants.TRAINING_TITLE_KEY_STRING, trainingSnapshot.child(Constants.TRAINING_TITLE_KEY_STRING).getValue(String.class));
        training.put(Constants.TRAINING_TYPE_KEY_STRING, trainingSnapshot.child(Constants.TRAINING_TYPE_KEY_STRING).getValue(String.class));
        training.put(Constants.TRAINING_DUE_TO_KEY_STRING, trainingSnapshot.child(Constants.TRAINING_DUE_TO_KEY_STRING).getValue(String.class));
        training.put(Constants.TRAINING_TARGET_KEY_STRING, trainingSnapshot.child(Constants.TRAINING_TARGET_KEY_STRING).getValue(String.class));
        training.put(
                Constants.TRAINING_COMPLETED_KEY_STRING,
                String.valueOf(trainingSnapshot.child(Constants.TRAINING_COMPLETED_KEY_STRING).getValue(Boolean.class))
        );
        training.put(Constants.TRAINING_UUID_KEY_STRING, trainingSnapshot.getKey());
        return training;
    }
}