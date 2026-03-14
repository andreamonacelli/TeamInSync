package com.monacdev.teaminsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.viewholders.TrainingTileViewHolder;

import java.util.ArrayList;
import java.util.HashMap;

public class TrainingListTileAdapter extends RecyclerView.Adapter<TrainingTileViewHolder> {
    private ArrayList<HashMap<String, String>> trainingsList;
    private boolean activateActions;

    public TrainingListTileAdapter(ArrayList<HashMap<String, String>> trainingsList, boolean activateActions){
        this.trainingsList = trainingsList;
        this.activateActions = activateActions;
    }

    /**
     * Handles the creation of a new TrainingTileViewHolder accordingly
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return the newly created TrainingTileViewHolder
     */
    @NonNull
    @Override
    public TrainingTileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.training_tile, parent, false);
        return new TrainingTileViewHolder(view, this.activateActions);
    }

    /**
     * Handles the filling of data for the TrainingTileViewHolder in a specific position
     * @param trainingTileViewHolder The TrainingTileViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull TrainingTileViewHolder trainingTileViewHolder, int position) {
        HashMap<String, String> trainingData = this.trainingsList.get(position);
        trainingTileViewHolder.bindData(trainingData);
    }

    /**
     * Fetches the number of items within the output list
     * @return the number of items in the list
     */
    @Override
    public int getItemCount() {
        if(this.trainingsList != null){
            return this.trainingsList.size();
        }
        return 0;
    }
}
