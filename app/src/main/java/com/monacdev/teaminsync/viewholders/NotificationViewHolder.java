package com.monacdev.teaminsync.viewholders;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.NotificationsKeys;
import com.monacdev.teaminsync.fragments.NotificationsFragment;

import java.util.HashMap;
import java.util.Objects;

public class NotificationViewHolder extends RecyclerView.ViewHolder {
    private TextView notificationTitleTV;
    private TextView notificationMessageTV;
    private ImageButton markAsReadBtn;
    private boolean isRead;
    private String notificationAthleteID;
    private String notificationUID;
    private HashMap<String, Object> currentNotificationData;
    private final NotificationsFragment parentFragment;

    public NotificationViewHolder(@NonNull View itemView, NotificationsFragment parentFragment) {
        super(itemView);
        this.parentFragment = parentFragment;
        this.bindViewsToObjects(itemView);
    }

    /**
     * Given the respective XML file, this method binds each view to its corresponding Java object
     * @param view the view object to which the TrainingTileViewHolder is bound
     */
    private void bindViewsToObjects(@NonNull View view){
        this.notificationTitleTV = view.findViewById(R.id.notificationTitleTV);
        this.notificationMessageTV = view.findViewById(R.id.notificationMessageTV);
        this.markAsReadBtn = view.findViewById(R.id.markAsReadBtn);
    }

    /**
     * Handles the binding of given notification data to the actual view that will be drawn on screen
     * @param notificationData the data to be shown in the view
     */
    public void bindData(@NonNull HashMap<String, Object> notificationData){
        this.currentNotificationData = notificationData;
        String title = Objects.requireNonNull(notificationData.get(NotificationsKeys.NOTIFICATIONS_TITLE)).toString();
        String message = Objects.requireNonNull(notificationData.get(NotificationsKeys.NOTIFICATIONS_MSG)).toString();
        this.notificationTitleTV.setText(title);
        this.notificationMessageTV.setText(message);
        this.notificationUID = Objects.requireNonNull(notificationData.get(NotificationsKeys.NOTIFICATIONS_UID)).toString();
        this.notificationAthleteID = Objects.requireNonNull(notificationData.get(KeyStrings.USERNAME)).toString();
        this.isRead = Boolean.parseBoolean(Objects.requireNonNull(notificationData.get(NotificationsKeys.NOTIFICATIONS_READ)).toString());
        this.itemView.setOnClickListener(view -> {
            this.parentFragment.showNotificationsDetails(title, message);
            if(!this.isRead){
                this.isRead = true;
                this.currentNotificationData.put(NotificationsKeys.NOTIFICATIONS_READ, true);
                this.updateNotificationDataOnDB(view, true, false);
                this.configureImageButton();
            }
        });
        this.configureImageButton();
    }

    /**
     * Based on the current status of the notification (isRead) define how to color the icon and what action perform when the button is clicked
     */
    private void configureImageButton(){
        if(this.isRead){
            this.markAsReadBtn.setColorFilter(Color.BLACK);
            this.markAsReadBtn.setOnClickListener(view -> {
                NotificationViewHolder.this.isRead = false;
                NotificationViewHolder.this.currentNotificationData.put(NotificationsKeys.NOTIFICATIONS_READ, false);
                NotificationViewHolder.this.markAsReadBtn.setColorFilter(Color.RED);
                NotificationViewHolder.this.updateNotificationDataOnDB(view, false, true);
                NotificationViewHolder.this.configureImageButton();
            });
        } else {
            this.markAsReadBtn.setColorFilter(Color.RED);
            this.markAsReadBtn.setOnClickListener(view -> {
                NotificationViewHolder.this.isRead = true;
                NotificationViewHolder.this.currentNotificationData.put(NotificationsKeys.NOTIFICATIONS_READ, true);
                NotificationViewHolder.this.markAsReadBtn.setColorFilter(Color.BLACK);
                NotificationViewHolder.this.updateNotificationDataOnDB(view, true, true);
                NotificationViewHolder.this.configureImageButton();
            });
        }
    }

    /**
     * Toggles the status (read/unread) of the notification on the DB
     * @param view the view over which display the Toast message
     * @param newStatus the status that has to be put on the DB
     */
    private void updateNotificationDataOnDB(View view, boolean newStatus, boolean fromIcon){
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference();
        notificationRef.child(ReferenceStrings.NOTIFICATIONS).child(this.notificationAthleteID).child(this.notificationUID)
                .child(NotificationsKeys.NOTIFICATIONS_READ).setValue(newStatus).addOnSuccessListener(unused -> {
                    if(fromIcon){
                        if(newStatus){
                            Toast.makeText(view.getContext(), R.string.marked_as_read, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(view.getContext(), R.string.marked_unread, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(e -> Toast.makeText(view.getContext(), R.string.upload_error_label, Toast.LENGTH_SHORT).show());
    }
}
