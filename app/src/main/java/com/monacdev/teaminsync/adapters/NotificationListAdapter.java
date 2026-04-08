package com.monacdev.teaminsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.viewholders.NotificationViewHolder;

import java.util.ArrayList;
import java.util.HashMap;

public class NotificationListAdapter extends RecyclerView.Adapter<NotificationViewHolder> {
    private final ArrayList<HashMap<String, Object>> notificationsList;

    public NotificationListAdapter(ArrayList<HashMap<String, Object>> notificationsList){
        this.notificationsList = notificationsList;
    }

    /**
     * Handles the creation of a new NotificationViewHolder accordingly
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return the newly created NotificationViewHolder
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_tile, parent, false);
        return new NotificationViewHolder(view);
    }

    /**
     * Handles the filling of data for the NotificationViewHolder in a specific position
     * @param notificationViewHolder The NotificationViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder notificationViewHolder, int position) {
        HashMap<String, Object> notificationData = this.notificationsList.get(position);
        notificationViewHolder.bindData(notificationData);
    }

    /**
     * Fetches the number of items within the output list
     * @return the number of items in the list
     */
    @Override
    public int getItemCount() {
        if(this.notificationsList != null){
            return this.notificationsList.size();
        }
        return 0;
    }
}
