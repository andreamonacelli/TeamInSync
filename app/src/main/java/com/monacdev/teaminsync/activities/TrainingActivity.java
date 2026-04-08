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
import com.monacdev.teaminsync.adapters.TrainingListTileAdapter;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.fragments.TrainingCreationWizardFragment;
import com.monacdev.teaminsync.constants.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class TrainingActivity extends AppCompatActivity {
    private TextView trainingPageHeaderTV;
    private RecyclerView trainingListRV;
    private View trainingTrackerFragmentContainer;
    private ImageButton createTrainingBtn;
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private String displayedUserID;
    private String viewedTeamID;
    private boolean isMyTrainingList;

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

        this.displayedUserID = getIntent().getStringExtra(IntentExtrasTags.DISPLAYED_USER);
        String displayedUserSurname = getIntent().getStringExtra(IntentExtrasTags.DISPLAYED_USER_SURNAME);
        this.bindViewsToObjects();
        this.setListeners();
        this.isDisplayedUserLogged();
        this.trainingPageHeaderTV.setText(String.format("%s %s", getString(R.string.training_list_header), displayedUserSurname));
        this.trainingTrackerFragmentContainer = findViewById(R.id.trainingTrackerFragmentContainer);
        this.trainingTrackerFragmentContainer.setVisibility(View.GONE);
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
            DatabaseReference displayedUserRef = this.dbRef.child(ReferenceStrings.USERS).child(this.displayedUserID);
            displayedUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String displayedUserRole = null;
                    if(snapshot.exists()){
                        viewedTeamID = snapshot.child(KeyStrings.TEAM).getValue(String.class);
                        String displayedUserEmail = snapshot.child(KeyStrings.EMAIL).getValue(String.class);
                        displayedUserRole = snapshot.child(KeyStrings.ROLE).getValue(String.class);
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
            /* Actually useful only for buttons visibility's sake */
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
        this.createTrainingBtn.setOnClickListener(view -> {
            TrainingCreationWizardFragment trainingCreationWizard = TrainingCreationWizardFragment.newInstance(TrainingActivity.this.viewedTeamID, TrainingActivity.this.displayedUserID);
            trainingCreationWizard.show(getSupportFragmentManager(), NavigationTags.TRAINING_CREATION_WIZARD);
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
            q = this.dbRef.child(ReferenceStrings.TRAININGS).child(displayedUserID);
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
            q = this.dbRef.child(ReferenceStrings.TRAININGS);
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
        training.put(KeyStrings.TRAINING_TITLE, trainingSnapshot.child(KeyStrings.TRAINING_TITLE).getValue(String.class));
        training.put(KeyStrings.TRAINING_TYPE, trainingSnapshot.child(KeyStrings.TRAINING_TYPE).getValue(String.class));
        training.put(KeyStrings.TRAINING_DUE_TO, trainingSnapshot.child(KeyStrings.TRAINING_DUE_TO).getValue(String.class));
        training.put(KeyStrings.TRAINING_TARGET, trainingSnapshot.child(KeyStrings.TRAINING_TARGET).getValue(String.class));
        training.put(
                KeyStrings.TRAINING_COMPLETED,
                String.valueOf(trainingSnapshot.child(KeyStrings.TRAINING_COMPLETED).getValue(Boolean.class))
        );
        training.put(KeyStrings.USERNAME, this.displayedUserID);
        training.put(KeyStrings.TRAINING_UUID, trainingSnapshot.getKey());
        return training;
    }
}