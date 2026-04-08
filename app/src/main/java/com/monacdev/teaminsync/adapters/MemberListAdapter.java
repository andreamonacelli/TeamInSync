package com.monacdev.teaminsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monacdev.teaminsync.viewholders.MemberViewHolder;
import com.monacdev.teaminsync.R;

import java.util.ArrayList;
import java.util.HashMap;

public class MemberListAdapter extends RecyclerView.Adapter<MemberViewHolder> {
    private final ArrayList<HashMap<String, String>> membersList;

    public MemberListAdapter(ArrayList<HashMap<String, String>> membersList){
        this.membersList = membersList;
    }

    /**
     * Handles the creation of a new MemberViewHolder accordingly
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return the newly created MemberViewHolder object
     */
    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_tile, parent, false);
        return new MemberViewHolder(view);
    }

    /**
     * Handles the filling of data for the MemberViewHolder in a specific position
     * @param memberViewHolder The MemberViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder memberViewHolder, int position) {
        HashMap<String, String> userData = this.membersList.get(position);
        memberViewHolder.bindData(userData);
    }

    /**
     * Fetches the number of items within the output list
     * @return the number of items in the list
     */
    @Override
    public int getItemCount() {
        if(this.membersList != null){
            return this.membersList.size();
        }
        return 0;
    }
}
