package com.monacdev.teaminsync.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.NotificationsKeys;
import com.monacdev.teaminsync.loaders.LoaderDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class NotificationsFragment extends DialogFragment {
    private TextView emptyNotificationsTV;
    private RecyclerView notificationsListRV;
    private ImageButton closeNotificationsBtn;
    private View notificationsDetailLayout;
    private TextView notificationDetailTitleTV;
    private TextView notificationDetailMessageTV;
    private Button backToNotificationsListBtn;
    private String loggedUsername;
    private boolean showingNotificationDetails = false;
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private LoaderDialog loaderDialog;

    public static NotificationsFragment newInstance(String loggedUsername){
        NotificationsFragment fragment = new NotificationsFragment();
        Bundle args = new Bundle();
        args.putString(KeyStrings.USERNAME, loggedUsername);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            this.loggedUsername = getArguments().getString(KeyStrings.USERNAME);
        }
        this.loaderDialog = new LoaderDialog(requireActivity());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_notifications, null);
        this.bindViewsToObjects(view);
        this.setListeners();
        if(savedInstanceState != null){
            this.showingNotificationDetails = savedInstanceState.getBoolean(IntentExtrasTags.NOTIFICATIONS_DETAILS_STATE_KEY);
            if(this.showingNotificationDetails){
                String savedTitle = savedInstanceState.getString(IntentExtrasTags.NOTIFICATIONS_TITLE_STATE_KEY);
                String savedMessage = savedInstanceState.getString(IntentExtrasTags.NOTIFICATIONS_MESSAGE_STATE_KEY);
                this.showNotificationsDetails(savedTitle, savedMessage);
            }
        }
        this.fetchNotifications();
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IntentExtrasTags.NOTIFICATIONS_DETAILS_STATE_KEY, this.showingNotificationDetails);
        if(this.showingNotificationDetails){
            outState.putString(IntentExtrasTags.NOTIFICATIONS_TITLE_STATE_KEY, this.notificationDetailTitleTV.getText().toString());
            outState.putString(IntentExtrasTags.NOTIFICATIONS_MESSAGE_STATE_KEY, this.notificationDetailMessageTV.getText().toString());
        }
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
        this.notificationsDetailLayout = view.findViewById(R.id.notificationDetailLayout);
        this.notificationDetailTitleTV = view.findViewById(R.id.notificationDetailTitleTV);
        this.notificationDetailMessageTV = view.findViewById(R.id.notificationDetailMessageTV);
        this.backToNotificationsListBtn = view.findViewById(R.id.backToNotificationsListBtn);
    }

    /**
     * Defines the listeners for the View components within the current activity
     */
    private void setListeners(){
        this.closeNotificationsBtn.setOnClickListener(view -> dismiss());
        this.backToNotificationsListBtn.setOnClickListener(view -> {
            this.showingNotificationDetails = false;
            this.notificationsDetailLayout.setVisibility(View.GONE);
            this.notificationsListRV.setVisibility(View.VISIBLE);
        });
    }

    /**
     * Given the logged user, fetches the list of notifications associated to it
     */
    private void fetchNotifications(){
        this.loaderDialog.show(getString(R.string.notifications_load_msg));
        this.dbRef.child(ReferenceStrings.NOTIFICATIONS).child(this.loggedUsername).orderByChild(NotificationsKeys.NOTIFICATIONS_TIMESTAMP)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        NotificationsFragment.this.loaderDialog.hide();
                        ArrayList<HashMap<String, Object>> notificationsList = new ArrayList<>();
                        if(snapshot.exists()){
                            for(DataSnapshot notificationSnapshot : snapshot.getChildren()){
                                HashMap<String, Object> notificationData = NotificationsFragment.this.parseNotification(notificationSnapshot);
                                notificationsList.add(notificationData);
                            }
                            Collections.reverse(notificationsList);
                            NotificationListAdapter notificationsAdapter = new NotificationListAdapter(notificationsList, NotificationsFragment.this);
                            NotificationsFragment.this.notificationsListRV.setAdapter(notificationsAdapter);
                            if(!NotificationsFragment.this.showingNotificationDetails) {
                                NotificationsFragment.this.notificationsListRV.setVisibility(View.VISIBLE);
                                NotificationsFragment.this.emptyNotificationsTV.setVisibility(View.GONE);
                            }
                        } else {
                            if(!NotificationsFragment.this.showingNotificationDetails) {
                                /* If there are no notifications for the current user display the respective TextView */
                                NotificationsFragment.this.notificationsListRV.setVisibility(View.GONE);
                                NotificationsFragment.this.emptyNotificationsTV.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        NotificationsFragment.this.loaderDialog.hide();
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
        notificationData.put(NotificationsKeys.NOTIFICATIONS_UID, notificationSnapshot.getKey());
        notificationData.put(KeyStrings.USERNAME, this.loggedUsername);
        notificationData.put(NotificationsKeys.NOTIFICATIONS_TITLE, notificationSnapshot.child(NotificationsKeys.NOTIFICATIONS_TITLE).getValue(String.class));
        notificationData.put(NotificationsKeys.NOTIFICATIONS_MSG, notificationSnapshot.child(NotificationsKeys.NOTIFICATIONS_MSG).getValue(String.class));
        Boolean isRead = notificationSnapshot.child(NotificationsKeys.NOTIFICATIONS_READ).getValue(Boolean.class);
        if(isRead == null){
            isRead = false;
        }
        notificationData.put(NotificationsKeys.NOTIFICATIONS_READ, isRead);
        return notificationData;
    }

    /**
     * Displays the details of the clicked notification over the current fragment space
     * @param title the title of the selected notification
     * @param message the content of the selected notification
     */
    public void showNotificationsDetails(String title, String message){
        this.showingNotificationDetails = true;
        this.notificationsListRV.setVisibility(View.GONE);
        this.notificationDetailTitleTV.setText(title);
        this.notificationDetailMessageTV.setText(message);
        this.notificationsDetailLayout.setVisibility(View.VISIBLE);
    }
}
