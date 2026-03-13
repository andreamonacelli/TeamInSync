package com.monacdev.teaminsync;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.monacdev.teaminsync.utils.Constants;

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
    private boolean actionsActive;
    private String trainingUID;

    public TrainingTileViewHolder(@NonNull View itemView, boolean actionsActive) {
        super(itemView);
        this.bindViewsToObjects(itemView);
        this.actionsActive = actionsActive;
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
    public void bindData(HashMap<String, String> trainingData){
        this.fillTextFields(
                trainingData.get(Constants.TRAINING_TITLE_KEY_STRING),
                trainingData.get(Constants.TRAINING_TARGET_KEY_STRING),
                trainingData.get(Constants.TRAINING_DUE_TO_KEY_STRING)
        );
        this.trainingUID = trainingData.get(Constants.TRAINING_UUID_KEY_STRING);
        this.handleTrainingType(trainingData.get(Constants.TRAINING_TYPE_KEY_STRING), trainingData.get(Constants.USERNAME_KEY_STRING));
        this.isExpired = isTrainingExpired(trainingData.get(Constants.TRAINING_DUE_TO_KEY_STRING));
        this.isCompleted = Boolean.parseBoolean(trainingData.get(Constants.TRAINING_COMPLETED_KEY_STRING));
        if(this.actionsActive){
            this.startTrainingBtn.setVisibility(View.VISIBLE);
        } else {
            this.startTrainingBtn.setVisibility(View.GONE);
        }
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
            this.startTrainingBtn.setOnClickListener(null);
            this.startTrainingBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /* TODO: Start the training tracking in a fragment */
                }
            });
        } else {
            this.trainingTypeIconIV.setImageResource(R.drawable.ic_weight);
            this.startTrainingBtn.setText(R.string.complete_training_label);
            this.startTrainingBtn.setOnClickListener(null);
            this.startTrainingBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(TrainingTileViewHolder.this.trainingUID != null && userID != null){
                        TrainingTileViewHolder.this.updateTrainingOnDB(view, userID);
                    } else {
                        Toast.makeText(view.getContext(), R.string.training_completion_error, Toast.LENGTH_SHORT).show();
                    }
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
        DatabaseReference trainingRef = FirebaseDatabase.getInstance().getReference();
        trainingRef.child(Constants.TRAININGS_REFERENCE_STRING).child(userID).child(TrainingTileViewHolder.this.trainingUID)
                .child(Constants.TRAINING_COMPLETED_KEY_STRING).setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        TrainingTileViewHolder.this.isCompleted = true;
                        TrainingTileViewHolder.this.restyleBasedOnCompletionExpiry();
                        if(TrainingTileViewHolder.this.isExpired) {
                            Toast.makeText(view.getContext(), R.string.training_reps_completed_late, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(view.getContext(), R.string.training_reps_completed_on_time, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        TrainingTileViewHolder.this.isCompleted = false;
                        Toast.makeText(view.getContext(), R.string.upload_error_label, Toast.LENGTH_SHORT).show();
                    }
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
            if(deadlineDate != null && deadlineDate.after(currentDate)){
                return false;
            } else {
                return true;
            }
        } catch(ParseException e) {
            return true;
        }
    }

    /**
     * Based on whether the currently displayed exercise is completed or expired, restyle the tile
     */
    private void restyleBasedOnCompletionExpiry(){
        if(this.isExpired){
            if(this.isCompleted){
                /* Make the button non-clickable and color background in soft orange */
                this.startTrainingBtn.setClickable(false);
                this.itemView.setBackgroundColor(Color.YELLOW);
            } else {
                /* Make the button clickable and color background in red */
                this.startTrainingBtn.setClickable(true);
                this.itemView.setBackgroundColor(Color.RED);
            }
        } else {
            if(this.isCompleted){
                /* Make the button non-clickable and color background in light gray */
                this.startTrainingBtn.setClickable(false);
                this.itemView.setBackgroundColor(Color.LTGRAY);
            } else {
                /* Make the button clickable and color background in green */
                this.startTrainingBtn.setClickable(true);
                this.itemView.setBackgroundColor(Color.GREEN);
            }
        }
    }
}
