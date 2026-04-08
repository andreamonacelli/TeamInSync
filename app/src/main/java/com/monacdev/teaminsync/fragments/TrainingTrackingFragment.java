package com.monacdev.teaminsync.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.Constants;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.utils.TrainingTrackerManager;

import java.util.Locale;

public class TrainingTrackingFragment extends Fragment {
    private TextView workoutProgressTV;
    private ImageButton stopResumeTrainingBtn;
    private TextView exerciseNameTV;
    private TextView exerciseTargetTV;
    private Button completeTrainingBtn;
    private String trainingUID;
    private String athleteUsername;
    private TrainingTrackerManager trainingTrackerManager;
    private Handler timerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == Constants.MSG_UPDATE_TIMER){
                String timeString = (String) msg.obj;
                if(TrainingTrackingFragment.this.workoutProgressTV != null){
                    TrainingTrackingFragment.this.workoutProgressTV.setText(timeString);
                }
            } else if(msg.what == Constants.MSG_TRAINING_TARGET_REACHED){
                if(TrainingTrackingFragment.this.completeTrainingBtn != null){
                    TrainingTrackingFragment.this.completeTrainingBtn.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.training_completed_label), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    public static TrainingTrackingFragment newInstance(String trainingUID, String athleteUsername){
        TrainingTrackingFragment fragment = new TrainingTrackingFragment();
        Bundle args = new Bundle();
        args.putString(KeyStrings.TRAINING_UUID, trainingUID);
        args.putString(IntentExtrasTags.LOGGED_USER, athleteUsername);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.trainingUID = getArguments().getString(KeyStrings.TRAINING_UUID);
            this.athleteUsername = getArguments().getString(IntentExtrasTags.LOGGED_USER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training_tracking, container, false);
        this.bindViewsToObjects(view);
        this.fetchTrainingData();
        this.configureBackPressHandler();
        this.setListeners();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        /* Before destroying the view we make sure to clean up timer-related stuff */
        if(this.trainingTrackerManager != null){
            this.trainingTrackerManager.pauseTimer();
        }
        if(this.timerHandler != null){
            this.timerHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     * @param view The view to be taken as reference for the binding
     */
    private void bindViewsToObjects(View view){
        this.workoutProgressTV = view.findViewById(R.id.workoutProgressTV);
        this.stopResumeTrainingBtn = view.findViewById(R.id.stopResumeTrainingBtn);
        this.exerciseNameTV = view.findViewById(R.id.exerciseNameTV);
        this.exerciseTargetTV = view.findViewById(R.id.exerciseTargetTV);
        this.completeTrainingBtn = view.findViewById(R.id.completeTrainingBtn);
        this.completeTrainingBtn.setEnabled(false);
    }

    /**
     * Defines the listeners for the View components within the current fragment
     */
    private void setListeners(){
        this.stopResumeTrainingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TrainingTrackingFragment.this.trainingTrackerManager.isTimerRunning()){
                    TrainingTrackingFragment.this.trainingTrackerManager.pauseTimer();
                    TrainingTrackingFragment.this.stopResumeTrainingBtn.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    TrainingTrackingFragment.this.trainingTrackerManager.startTimer();
                    TrainingTrackingFragment.this.stopResumeTrainingBtn.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });
        this.completeTrainingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrainingTrackingFragment.this.flagTrainingAsCompleted();
                Toast.makeText(getContext(), getString(R.string.training_auto_completed), Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    /**
     * Handles the initialization of the TrainingTrackerManager that will deal with the timer
     */
    private void initializeTrackerManager(int targetSeconds){
        this.trainingTrackerManager = new TrainingTrackerManager(this.timerHandler);
        this.trainingTrackerManager.setTargetSeconds(targetSeconds);
        this.trainingTrackerManager.startTimer();
        this.stopResumeTrainingBtn.setImageResource(android.R.drawable.ic_media_pause);
    }

    /**
     * Deals with the consistency of data in case a back press is performed
     */
    private void configureBackPressHandler(){
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(TrainingTrackingFragment.this.trainingTrackerManager != null && TrainingTrackingFragment.this.trainingTrackerManager.isTargetReached()){
                    Toast.makeText(getContext(), getString(R.string.training_auto_completed), Toast.LENGTH_SHORT).show();
                    TrainingTrackingFragment.this.flagTrainingAsCompleted();
                } else {
                    Toast.makeText(getContext(), getString(R.string.training_canceled_label), Toast.LENGTH_SHORT).show();
                }
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * Fetches the details related to the displayed training directly from the Firebase DB
     */
    private void fetchTrainingData(){
        DatabaseReference trainingRef = FirebaseDatabase.getInstance().getReference().child(ReferenceStrings.TRAININGS).child(this.athleteUsername).child(this.trainingUID);
        trainingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String exerciseName = snapshot.child(KeyStrings.TRAINING_TITLE).getValue(String.class);
                    if(exerciseName != null){
                        TrainingTrackingFragment.this.exerciseNameTV.setText(exerciseName);
                    }
                    String targetValueString = snapshot.child(KeyStrings.TRAINING_TARGET).getValue(String.class);
                    if(targetValueString != null){
                        int targetValue = Integer.parseInt(targetValueString);
                        int targetSeconds = targetValue * 60;
                        TrainingTrackingFragment.this.exerciseTargetTV.setText(String.format(Locale.getDefault(), "%s -> %s", getString(R.string.tracking_frag_target_placeholder), TrainingTrackingFragment.this.formatTargetIntoHHMM(targetValue)));
                        TrainingTrackingFragment.this.initializeTrackerManager(targetSeconds);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.no_trainings_found, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Flags the current training as completed on the Firebase DB
     */
    private void flagTrainingAsCompleted(){
        /* Sending a Bundle to the activity in order to invoke the training completion */
        Bundle trainingResult = new Bundle();
        trainingResult.putBoolean(IntentExtrasTags.TRAINING_COMPLETED, true);
        requireActivity().getSupportFragmentManager().setFragmentResult(NavigationTags.TRAINING_BUNDLE_RESULT, trainingResult);
        DatabaseReference trainingRef = FirebaseDatabase.getInstance().getReference().child(ReferenceStrings.TRAININGS).child(this.athleteUsername).child(this.trainingUID);
        trainingRef.child(KeyStrings.TRAINING_COMPLETED).setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.i("TRAINING_COMPLETED", "Training has been flagged as completed");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), R.string.upload_error_label, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Given the target value in minutes it formats such value into a HH:MM string
     * @param targetValue the target value in minutes
     * @return a HH:MM formatted string holding a user readable value
     */
    private String formatTargetIntoHHMM(int targetValue){
        int targetMinutes = targetValue % 60;
        int targetHours = targetValue / 60;
        return String.format(Locale.getDefault(), "%02d:%02d", targetHours, targetMinutes);
    }
}
