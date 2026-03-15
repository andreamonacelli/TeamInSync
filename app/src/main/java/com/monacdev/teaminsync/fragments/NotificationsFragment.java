package com.monacdev.teaminsync.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.adapters.NotificationListAdapter;
import com.monacdev.teaminsync.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class NotificationsFragment extends DialogFragment {
    private TextView emptyNotificationsTV;
    private RecyclerView notificationsListRV;
    private ImageButton closeNotificationsBtn;
    private String loggedUsername;
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

    public static NotificationsFragment newInstance(String loggedUsername){
        NotificationsFragment fragment = new NotificationsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.USERNAME_KEY_STRING, loggedUsername);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.loggedUsername = getArguments().getString(Constants.USERNAME_KEY_STRING);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_notifications, null);
        this.bindViewsToObjects(view);
        this.setListeners();
        this.fetchNotifications();
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     * @param view The view to be taken as reference for the binding
     */
    private void bindViewsToObjects(View view){
        this.emptyNotificationsTV = view.findViewById(R.id.emptyNotificationsTV);
        this.notificationsListRV = view.findViewById(R.id.notificationsListRV);
        this.notificationsListRV.setLayoutManager(new LinearLayoutManager(getContext()));
        this.closeNotificationsBtn = view.findViewById(R.id.closeNotificationsBtn);
    }

    /**
     * Defines the listeners for the View components within the current activity
     */
    private void setListeners(){
        this.closeNotificationsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    /**
     * Given the logged user, fetches the list of notifications associated to it
     */
    private void fetchNotifications(){
        this.dbRef.child(Constants.NOTIFICATIONS_REFERENCE_STRING).child(this.loggedUsername).orderByChild(Constants.NOTIFICATIONS_TIMESTAMP_KEY)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<HashMap<String, Object>> notificationsList = new ArrayList<>();
                        if(snapshot.exists()){
                            for(DataSnapshot notificationSnapshot : snapshot.getChildren()){
                                HashMap<String, Object> notificationData = NotificationsFragment.this.parseNotification(notificationSnapshot);
                                notificationsList.add(notificationData);
                            }
                            Collections.reverse(notificationsList);
                            NotificationListAdapter notificationsAdapter = new NotificationListAdapter(notificationsList);
                            NotificationsFragment.this.notificationsListRV.setAdapter(notificationsAdapter);
                            NotificationsFragment.this.notificationsListRV.setVisibility(View.VISIBLE);
                            NotificationsFragment.this.emptyNotificationsTV.setVisibility(View.GONE);
                        } else {
                            /* If there are no notifications for the current user display the respective TextView */
                            NotificationsFragment.this.notificationsListRV.setVisibility(View.GONE);
                            NotificationsFragment.this.emptyNotificationsTV.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), R.string.connection_err, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Given a DataSnapshot received from the Firebase DB, parses the respective data into a HashMap to be given to the RecyclerView adapter
     * @param notificationSnapshot the DataSnapshot received from Firebase
     * @return a HashMap containing all the parsed data
     */
    private HashMap<String, Object> parseNotification(DataSnapshot notificationSnapshot){
        HashMap<String, Object> notificationData = new HashMap<>();
        notificationData.put(Constants.NOTIFICATIONS_UID_KEY, notificationSnapshot.getKey());
        notificationData.put(Constants.USERNAME_KEY_STRING, this.loggedUsername);
        notificationData.put(Constants.NOTIFICATIONS_TITLE_KEY, notificationSnapshot.child(Constants.NOTIFICATIONS_TITLE_KEY).getValue(String.class));
        notificationData.put(Constants.NOTIFICATIONS_MSG_KEY, notificationSnapshot.child(Constants.NOTIFICATIONS_MSG_KEY).getValue(String.class));
        Boolean isRead = notificationSnapshot.child(Constants.NOTIFICATIONS_READ_KEY).getValue(Boolean.class);
        if(isRead == null){
            isRead = false;
        }
        notificationData.put(Constants.NOTIFICATIONS_READ_KEY, isRead);
        return notificationData;
    }
}
