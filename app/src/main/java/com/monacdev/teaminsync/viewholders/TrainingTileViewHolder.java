package com.monacdev.teaminsync.viewholders;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.fragments.TrainingTrackingFragment;
import com.monacdev.teaminsync.constants.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class TrainingTileViewHolder extends RecyclerView.ViewHolder {
    private ImageView trainingTypeIconIV;
    private Button startTrainingBtn;
    private TextView trainingNameTV;
    private TextView trainingInfoTV;
    private boolean isExpired;
    private boolean isCompleted;
    private boolean isCompletedLate;
    private final boolean actionsActive;
    private final boolean isCoach;
    private String trainingUID;
    private String athleteUsername;

    public TrainingTileViewHolder(@NonNull View itemView, boolean actionsActive, boolean isCoach) {
        super(itemView);
        this.bindViewsToObjects(itemView);
        this.actionsActive = actionsActive;
        this.isCoach = isCoach;
    }

    /**
     * Given the respective XML file, this method binds each view to its corresponding Java object
     * @param view the view object to which the TrainingTileViewHolder is bound
     */
    private void bindViewsToObjects(@NonNull View view){
        this.trainingTypeIconIV = view.findViewById(R.id.trainingTypeIconIV);
        this.startTrainingBtn = view.findViewById(R.id.startTrainingBtn);
        this.trainingNameTV = view.findViewById(R.id.trainingNameTV);
        this.trainingInfoTV = view.findViewById(R.id.trainingInfoTV);
    }

    /**
     * Handles the binding of given training exercise data to the actual view that will be drawn on screen
     * @param trainingData the data to be shown in the view
     */
    public void bindData(@NonNull HashMap<String, String> trainingData){
        this.fillTextFields(
                trainingData.get(KeyStrings.TRAINING_TITLE),
                trainingData.get(KeyStrings.TRAINING_TARGET),
                trainingData.get(KeyStrings.TRAINING_DUE_TO)
        );
        this.trainingUID = trainingData.get(KeyStrings.TRAINING_UUID);
        this.athleteUsername = trainingData.get(KeyStrings.USERNAME);
        this.handleTrainingType(trainingData.get(KeyStrings.TRAINING_TYPE), trainingData.get(KeyStrings.USERNAME));
        this.isExpired = isTrainingExpired(trainingData.get(KeyStrings.TRAINING_DUE_TO));
        this.isCompleted = Boolean.parseBoolean(trainingData.get(KeyStrings.TRAINING_COMPLETED));
        this.isCompletedLate = Boolean.parseBoolean(trainingData.get(KeyStrings.TRAINING_COMPLETED_LATE));
        this.restyleBasedOnCompletionExpiry();
    }

    /**
     * Given the type of the training exercise that is going to be shown in the ViewHolder, it sets the respective icon
     * @param trainingType a string containing the type of training
     * @param userID the ID of the user for which we are showing the trainings
     */
    private void handleTrainingType(String trainingType, String userID){
        if(trainingType != null && trainingType.equals(Constants.CHRONO_TRAINING_STRING)){
            this.trainingTypeIconIV.setImageResource(R.drawable.ic_stopwatch);
            this.startTrainingBtn.setText(R.string.start_training_btn_label);
            this.startTrainingBtn.setOnClickListener(view -> {
                TrainingTrackingFragment trackingFragment = TrainingTrackingFragment.newInstance(TrainingTileViewHolder.this.trainingUID, TrainingTileViewHolder.this.athleteUsername);
                AppCompatActivity activity = null;
                Context context = view.getContext();
                while(context instanceof ContextWrapper){
                    if(context instanceof AppCompatActivity){
                        activity = (AppCompatActivity) context;
                        break;
                    }
                    context = ((ContextWrapper) context).getBaseContext();
                }
                if(activity != null) {
                    activity.getSupportFragmentManager().setFragmentResultListener(
                            NavigationTags.TRAINING_BUNDLE_RESULT,
                            activity,
                            (requestKey, result) -> {
                                boolean completed = result.getBoolean(IntentExtrasTags.TRAINING_COMPLETED);
                                if(completed){
                                    TrainingTileViewHolder.this.isCompleted = true;
                                    if(TrainingTileViewHolder.this.isExpired){
                                        TrainingTileViewHolder.this.isCompletedLate = true;
                                    }
                                    TrainingTileViewHolder.this.restyleBasedOnCompletionExpiry();
                                }
                            }
                    );
                    activity.findViewById(R.id.trainingTrackerFragmentContainer).setVisibility(View.VISIBLE);
                    activity.getSupportFragmentManager().beginTransaction().replace(R.id.trainingTrackerFragmentContainer, trackingFragment).addToBackStack(null).commit();
                } else {
                    Toast.makeText(view.getContext(), R.string.generic_error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            this.trainingTypeIconIV.setImageResource(R.drawable.ic_weight);
            this.startTrainingBtn.setText(R.string.complete_training_label);
            this.startTrainingBtn.setOnClickListener(view -> {
                if(TrainingTileViewHolder.this.trainingUID != null && userID != null){
                    TrainingTileViewHolder.this.updateTrainingOnDB(view, userID);
                    TrainingTileViewHolder.this.restyleBasedOnCompletionExpiry();
                } else {
                    Toast.makeText(view.getContext(), R.string.training_completion_error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Effectively update the training info on the Firebase DB
     * @param view the view over which display the Toast message
     * @param userID the ID of the user for whom the training should be completed
     */
    private void updateTrainingOnDB(View view, String userID){
        DatabaseReference trainingRef = FirebaseDatabase.getInstance().getReference().child(ReferenceStrings.TRAININGS).child(userID).child(TrainingTileViewHolder.this.trainingUID);
        HashMap<String, Object> trainingUpdateData = new HashMap<>();
        trainingUpdateData.put(KeyStrings.TRAINING_COMPLETED, true);
        trainingUpdateData.put(KeyStrings.TRAINING_COMPLETED_LATE, TrainingTileViewHolder.this.isExpired);
        trainingRef.updateChildren(trainingUpdateData).addOnSuccessListener(unused -> {
                    TrainingTileViewHolder.this.isCompleted = true;
                    TrainingTileViewHolder.this.isCompletedLate = TrainingTileViewHolder.this.isExpired;
                    TrainingTileViewHolder.this.restyleBasedOnCompletionExpiry();
                    if(TrainingTileViewHolder.this.isExpired) {
                        Toast.makeText(view.getContext(), R.string.training_reps_completed_late, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(view.getContext(), R.string.training_reps_completed_on_time, Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    TrainingTileViewHolder.this.isCompleted = false;
                    Toast.makeText(view.getContext(), R.string.upload_error_label, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Given the textual data to be displayed, this method effectively populates the views
     * @param title the name of the training exercise
     * @param target the target (which can be either in format hh:mm or an integer number)
     * @param deadline the due date for exercise completion
     */
    private void fillTextFields(String title, String target, String deadline){
        if(title != null){
            this.trainingNameTV.setText(title);
        } else {
            this.trainingNameTV.setText(R.string.training_name_placeholder);
        }
        if(target != null && deadline != null){
            this.trainingInfoTV.setText(String.format("Obiettivo: %s | Scadenza: %s", target, deadline));
        } else {
            this.trainingInfoTV.setText(R.string.training_info_placeholder);
        }
    }

    /**
     * Checks whether the shown exercise is expired or not
     * @param deadline the deadline date for the exercise
     * @return <strong>true</strong> if the exercise is late/expired, <strong>false</strong> otherwise
     */
    private boolean isTrainingExpired(String deadline){
        Date currentDate = new Date();
        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            simpleDateFormat.setLenient(false);
            Date deadlineDate = simpleDateFormat.parse(deadline);
            return !(deadlineDate != null && deadlineDate.after(currentDate));
        } catch(ParseException e) {
            return true;
        }
    }

    /**
     * Based on whether the currently displayed exercise is completed or expired, restyle the tile
     */
    private void restyleBasedOnCompletionExpiry(){
        Context context = this.itemView.getContext();
        MaterialCardView trainingCardView = (MaterialCardView) this.itemView;
        if(this.actionsActive){
            this.startTrainingBtn.setVisibility(View.VISIBLE);
        } else {
            trainingCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_active));
            this.startTrainingBtn.setVisibility(View.GONE);
            if(this.isCoach){
                return;
            }
        }
        if(this.isCompleted){
            this.startTrainingBtn.setVisibility(View.GONE);
            if(this.isCompletedLate){
                trainingCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_completed_late));
            } else {
                trainingCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_completed));
            }
        } else {
            if(this.actionsActive){
                this.startTrainingBtn.setVisibility(View.VISIBLE);
                this.startTrainingBtn.setEnabled(true);
            }
            if(this.isExpired){
                trainingCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_missed));
            } else {
                trainingCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.tile_active));
            }
        }
    }
}
