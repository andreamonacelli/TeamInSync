package com.monacdev.teaminsync.viewholders;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.activities.ProfileActivity;
import com.monacdev.teaminsync.constants.Constants;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;

import java.util.HashMap;

public class MemberViewHolder extends RecyclerView.ViewHolder {
    private ImageView memberProfilePicIV;
    private TextView memberNameTV;
    private TextView memberInfoTV;

    public MemberViewHolder(@NonNull View itemView) {
        super(itemView);
        this.bindViewsToObjects(itemView);
    }

    /**
     * Handles the binding of given user data to the actual view that will be drawn on screen
     * @param userData the data to be shown in the view
     */
    public void bindData(@NonNull HashMap<String, String> userData){
        this.setUpTextViews(
                userData.get(KeyStrings.NAME),
                userData.get(KeyStrings.SURNAME),
                userData.get(KeyStrings.USERNAME),
                userData.get(KeyStrings.BIRTHDATE)
        );
        this.setProfilePictureOnView(userData.get(KeyStrings.PROFILE_PIC), userData.get(KeyStrings.ROLE));
        this.itemView.setOnClickListener(view -> {
            Intent memberDetailsIntent = new Intent(itemView.getContext(), ProfileActivity.class);
            memberDetailsIntent.putExtra(IntentExtrasTags.DISPLAYED_USER, userData.get(KeyStrings.USERNAME));
            itemView.getContext().startActivity(memberDetailsIntent);
        });
    }

    /**
     * Sets up all the text views within the tile
     * @param name the name to be displayed
     * @param surname the surname to be displayed
     * @param username the username of the represented user
     * @param birthDate the birthdate of the represented user
     */
    private void setUpTextViews(String name, String surname, String username, String birthDate){
        if(name != null && surname != null){
            this.memberNameTV.setText(String.format("%s %s", name, surname));
        } else {
            this.memberNameTV.setText(R.string.member_tile_default);
        }
        if(birthDate == null){
            this.memberInfoTV.setText(username);
        } else if (username != null){
            this.memberInfoTV.setText(String.format("@%s | %s", username, birthDate));
        } else {
            this.memberInfoTV.setText(R.string.member_tile_info_def);
        }
    }

    /**
     * Fetches the profile picture from the remote storage and sets it on the view, eventually falling back to a default picture
     * @param imagePath the path to the remote picture
     */
    private void setProfilePictureOnView(String imagePath, String role){
        /* Handling fallback image in case of errors */
        int fallbackImageId;
        if(role != null && role.equals(Constants.COACH_ROLE_STRING)){
            fallbackImageId = R.drawable.ic_coach;
        } else {
            fallbackImageId = R.drawable.ic_athlete;
        }
        Glide.with(itemView.getContext()).load(imagePath)
                .placeholder(fallbackImageId)
                .error(fallbackImageId)
                .circleCrop()
                .into(this.memberProfilePicIV);
    }

    /**
     * Given the respective XML file, this method binds each view to its corresponding Java object
     * @param view the view object to which the MemberViewHolder is bound
     */
    private void bindViewsToObjects(@NonNull View view){
        this.memberProfilePicIV = view.findViewById(R.id.memberProfilePicIV);
        this.memberNameTV = view.findViewById(R.id.memberNameTV);
        this.memberInfoTV = view.findViewById(R.id.memberInfoTV);
    }
}
