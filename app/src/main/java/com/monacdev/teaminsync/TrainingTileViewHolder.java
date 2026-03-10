package com.monacdev.teaminsync;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

    public TrainingTileViewHolder(@NonNull View itemView) {
        super(itemView);
        this.bindViewsToObjects(itemView);
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
        this.handleTrainingType(trainingData.get(Constants.TRAINING_TYPE_KEY_STRING));
        this.isExpired = isTrainingExpired(trainingData.get(Constants.TRAINING_DUE_TO_KEY_STRING));
        this.isCompleted = Boolean.parseBoolean(trainingData.get(Constants.TRAINING_COMPLETED_KEY_STRING));
        this.restyleBasedOnCompletionExpiry();
    }

    /**
     * Given the type of the training exercise that is going to be shown in the ViewHolder, it sets the respective icon
     * @param trainingType a string containing the type of training
     */
    private void handleTrainingType(String trainingType){
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
                    /* TODO: simply flag the training as completed (on the DB as well) */
                }
            });
        }
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
