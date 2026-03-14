package com.monacdev.teaminsync.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.activities.LoginActivity;
import com.monacdev.teaminsync.utils.Constants;
import com.monacdev.teaminsync.utils.PushNotificationsManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TrainingCreationWizardFragment extends DialogFragment {
    private EditText trainingTitleET;
    private RadioGroup trainingTypeRG;
    private EditText trainingTargetET;
    private EditText trainingDueToET;
    private Button completeBtn;
    private Button appendBtn;
    private String teamID;
    private String coachID;
    private ArrayList<String> targetAthletes;
    private DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

    public static TrainingCreationWizardFragment newInstance(String teamID, String coachID) {
        final TrainingCreationWizardFragment fragment = new TrainingCreationWizardFragment();
        final Bundle args = new Bundle();
        args.putString(Constants.TEAM_KEY_STRING, teamID);
        args.putString(Constants.COACH_ROLE_STRING, coachID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.teamID = getArguments().getString(Constants.TEAM_KEY_STRING);
            this.coachID = getArguments().getString(Constants.COACH_ROLE_STRING);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_training_creation_wizard, null);
        this.bindViewsToObjects(view);
        this.setListeners();
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     * @param view The view to be taken as reference for the binding
     */
    private void bindViewsToObjects(View view){
        this.trainingTitleET = view.findViewById(R.id.trainingTitleET);
        this.trainingTypeRG = view.findViewById(R.id.trainingTypeRG);
        this.trainingTargetET = view.findViewById(R.id.trainingTargetET);
        this.trainingDueToET = view.findViewById(R.id.trainingDueToET);
        this.completeBtn = view.findViewById(R.id.completeBtn);
        this.appendBtn = view.findViewById(R.id.appendBtn);
        this.fetchTargetAthletes();
    }

    /**
     * Defines the listeners for the View components within the current activity
     */
    private void setListeners(){
        this.appendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadTrainingToDatabase(Constants.EFFECT_NEXT);
            }
        });
        this.completeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadTrainingToDatabase(Constants.EFFECT_DISMISS);
            }
        });
    }

    /**
     * Clears the input fields in order to allow the user to create another exercise
     */
    private void clearInputFields(){
        this.trainingTitleET.setText("");
        this.trainingTargetET.setText("");
        this.trainingDueToET.setText("");
    }

    /**
     * Given the team, fetches the list of athletes that will be targets for the training
     */
    private void fetchTargetAthletes(){
        this.targetAthletes = new ArrayList<>();
        Query teamRef = this.dbRef.child(Constants.USERS_REFERENCE_STRING).orderByChild(Constants.TEAM_KEY_STRING).equalTo(this.teamID);
        teamRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot memberSnapshot : snapshot.getChildren()){
                        String role = memberSnapshot.child(Constants.ROLE_KEY_STRING).getValue(String.class);
                        if(role != null && role.equals(Constants.PLAYER_ROLE_STRING)){
                            targetAthletes.add(memberSnapshot.getKey());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Uploads the newly created trainings to the Firebase DB
     */
    private void uploadTrainingToDatabase(String completionEffect){
        String trainingUID = UUID.randomUUID().toString();
        HashMap<String, Object> trainingData = this.prepareDataForUpload();
        if(trainingData.isEmpty()){
            /* Do not update if data is empty */
            return;
        }
        Map<String, Object> multiUploadMap = new HashMap<>();
        for(String athleteID : this.targetAthletes){
            /* Upload training data */
            String trainingPath = String.format("%s/%s/%s", Constants.TRAININGS_REFERENCE_STRING, athleteID, trainingUID);
            multiUploadMap.put(trainingPath, trainingData);
            if(completionEffect.equals(Constants.EFFECT_DISMISS)) {
                /* Upload notification data and send push notification */
                String notificationUID = UUID.randomUUID().toString();
                String notificationPath = String.format("%s/%s/%s", Constants.NOTIFICATIONS_REFERENCE_STRING, athleteID, notificationUID);
                HashMap<String, Object> notificationData = this.prepareNotification();
                multiUploadMap.put(notificationPath, notificationData);
            }
        }
        this.dbRef.updateChildren(multiUploadMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(getContext(), R.string.exercise_added, Toast.LENGTH_SHORT).show();
                    if(completionEffect.equals(Constants.EFFECT_DISMISS)){
                        dismiss();
                        PushNotificationsManager.sendPushNotification(
                                TrainingCreationWizardFragment.this.targetAthletes,
                                getString(R.string.push_new_training_title),
                                getString(R.string.push_new_training_body)
                        );
                    } else {
                        clearInputFields();
                    }
                } else {
                    Toast.makeText(getContext(), R.string.upload_error_label, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Creates a HashMap containing all the data related to the training, ready to be uploaded to the Firebase DB
     * @return the HashMap containing all the training data
     */
    private HashMap<String, Object> prepareDataForUpload(){
        HashMap<String, Object> trainingData = new HashMap<>();
        String title = this.trainingTitleET.getText().toString().trim();
        String target = this.trainingTargetET.getText().toString().trim();
        String dueToDate = this.trainingDueToET.getText().toString().trim();
        if(validateInputData(target, dueToDate)){
            String trainingType;
            if(this.trainingTypeRG.getCheckedRadioButtonId() == R.id.chronoRB){
                trainingType = Constants.CHRONO_TRAINING_STRING;
            } else {
                trainingType = Constants.REPS_TRAINING_STRING;
            }
            trainingData.put(Constants.TRAINING_TITLE_KEY_STRING, title);
            trainingData.put(Constants.TRAINING_TYPE_KEY_STRING, trainingType);
            trainingData.put(Constants.TRAINING_TARGET_KEY_STRING, target);
            trainingData.put(Constants.TRAINING_DUE_TO_KEY_STRING, dueToDate);
            trainingData.put(Constants.COACH_ROLE_STRING, this.coachID);
            trainingData.put(Constants.TRAINING_COMPLETED_KEY_STRING, false);
        }
        return trainingData;
    }

    /**
     * Performs a check over the received data in order to determine whether it is in a valid format or not
     * @param target the target for the training (it should be either an integer number or a string in HH:MM format)
     * @param dueToDate should be a valid date in the format YYYY-MM-DD
     * @return <strong>true</strong> if all data is format-compliant, <strong>false</strong> otherwise
     */
    private boolean validateInputData(String target, String dueToDate){
        if(target.isEmpty()){
            return false;
        }
        if(!target.matches("[0-9]*?[0-9]:[0-9][0-9]") && !target.matches("[0-9]+")){
            return false;
        }
        /* Here we can assume that the checks on the target string have passed */
        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            simpleDateFormat.setLenient(false);
            Date dueDate = simpleDateFormat.parse(dueToDate);
            return true;
        } catch(ParseException e) {
            return false;
        }
    }

    /**
     * Prepares a HashMap containing all the notification info to be uploaded on the Firebase DB
     * @return the filled HashMap object
     */
    private HashMap<String, Object> prepareNotification(){
        HashMap<String, Object> notificationData = new HashMap<>();
        notificationData.put(Constants.NOTIFICATIONS_TITLE_KEY, getString(R.string.push_new_training_title));
        notificationData.put(Constants.NOTIFICATIONS_MSG_KEY, getString(R.string.push_new_training_body));
        notificationData.put(Constants.NOTIFICATIONS_READ_KEY, false);
        notificationData.put(Constants.NOTIFICATIONS_TIMESTAMP_KEY, System.currentTimeMillis());
        return notificationData;
    }
}
