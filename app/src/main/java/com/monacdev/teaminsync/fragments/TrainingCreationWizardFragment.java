package com.monacdev.teaminsync.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.Constants;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.NotificationsKeys;
import com.monacdev.teaminsync.loaders.LoaderDialog;
import com.monacdev.teaminsync.utils.PushNotificationsManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private LoaderDialog loaderDialog;

    public static TrainingCreationWizardFragment newInstance(String teamID, String coachID) {
        final TrainingCreationWizardFragment fragment = new TrainingCreationWizardFragment();
        final Bundle args = new Bundle();
        args.putString(KeyStrings.TEAM, teamID);
        args.putString(Constants.COACH_ROLE_STRING, coachID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.teamID = getArguments().getString(KeyStrings.TEAM);
            this.coachID = getArguments().getString(Constants.COACH_ROLE_STRING);
        }
        this.loaderDialog = new LoaderDialog(requireActivity());
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
     * Defines the listeners for the View components within the current fragment
     */
    private void setListeners(){
        this.appendBtn.setOnClickListener(view -> this.uploadTrainingToDatabase(Constants.EFFECT_NEXT));
        this.completeBtn.setOnClickListener(view -> this.uploadTrainingToDatabase(Constants.EFFECT_DISMISS));
        this.trainingDueToET.setOnClickListener(view -> this.showExpirationDatePickerDialog());
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
        Query teamRef = this.dbRef.child(ReferenceStrings.USERS).orderByChild(KeyStrings.TEAM).equalTo(this.teamID);
        teamRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot memberSnapshot : snapshot.getChildren()){
                        String role = memberSnapshot.child(KeyStrings.ROLE).getValue(String.class);
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
        this.loaderDialog.show(getString(R.string.upload_training_loading_msg));
        Map<String, Object> multiUploadMap = new HashMap<>();
        for(String athleteID : this.targetAthletes){
            /* Upload training data */
            String trainingPath = String.format("%s/%s/%s", ReferenceStrings.TRAININGS, athleteID, trainingUID);
            multiUploadMap.put(trainingPath, trainingData);
            if(completionEffect.equals(Constants.EFFECT_DISMISS)) {
                /* Upload notification data and send push notification */
                String notificationUID = UUID.randomUUID().toString();
                String notificationPath = String.format("%s/%s/%s", ReferenceStrings.NOTIFICATIONS, athleteID, notificationUID);
                HashMap<String, Object> notificationData = this.prepareNotification();
                multiUploadMap.put(notificationPath, notificationData);
            }
        }
        this.dbRef.updateChildren(multiUploadMap).addOnCompleteListener(task -> {
            this.loaderDialog.hide();
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
            trainingData.put(KeyStrings.TRAINING_TITLE, title);
            trainingData.put(KeyStrings.TRAINING_TYPE, trainingType);
            trainingData.put(KeyStrings.TRAINING_TARGET, target);
            trainingData.put(KeyStrings.TRAINING_DUE_TO, dueToDate);
            trainingData.put(Constants.COACH_ROLE_STRING, this.coachID);
            trainingData.put(KeyStrings.TRAINING_COMPLETED, false);
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
            simpleDateFormat.parse(dueToDate);
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
        notificationData.put(NotificationsKeys.NOTIFICATIONS_TITLE, getString(R.string.push_new_training_title));
        notificationData.put(NotificationsKeys.NOTIFICATIONS_MSG, getString(R.string.push_new_training_body));
        notificationData.put(NotificationsKeys.NOTIFICATIONS_READ, false);
        notificationData.put(NotificationsKeys.NOTIFICATIONS_TIMESTAMP, System.currentTimeMillis());
        return notificationData;
    }

    /**
     * Displays a DatePickerDialog to allow the user to set the expiration date properly, regardless of
     * eventual differences in the keyboard format, date format or whatever
     */
    private void showExpirationDatePickerDialog(){
        /* In case we are in editing mode, we should open the Calendar/Date Picker on the previously inserted date, otherwise we use today's date */
        Calendar calendar = Calendar.getInstance();
        int displayedYear = calendar.get(Calendar.YEAR);
        int displayedMonth = calendar.get(Calendar.MONTH);
        int displayedDay = calendar.get(Calendar.DAY_OF_MONTH);
        String previousExpirationDate = this.trainingDueToET.getText().toString().trim();
        if(!previousExpirationDate.isEmpty()){
            try{
                String[] dateParts = previousExpirationDate.split("-");
                displayedYear = Integer.parseInt(dateParts[Constants.DATE_PART_YEAR]);
                displayedMonth = Integer.parseInt(dateParts[Constants.DATE_PART_MONTH]) - 1; // Months in Java go from 0 to 11
                displayedDay = Integer.parseInt(dateParts[Constants.DATE_PART_DAY]);
            } catch (Exception parsingException){
                Log.e("date_parse_error", "Error while parsing expiration date");
            }
        }
        DatePickerDialog expirationDatePicker = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            TrainingCreationWizardFragment.this.trainingDueToET.setText(formattedDate);
        }, displayedYear, displayedMonth, displayedDay);
        expirationDatePicker.show();
    }
}
