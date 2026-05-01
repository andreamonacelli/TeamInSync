package com.monacdev.teaminsync.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.adapters.MemberListAdapter;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.Constants;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.loaders.LoaderDialog;

import java.util.ArrayList;
import java.util.HashMap;

public class MembersListActivity extends AppCompatActivity {
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private String teamID;
    private String loggedUsername;
    private int completedRecyclerViews = 0;
    private ValueEventListener teamFetchListener;
    private ValueEventListener athletesFetchListener;
    private ValueEventListener coachesFetchListener;
    private LoaderDialog loaderDialog;
    private RecyclerView coachesListRV;
    private RecyclerView athletesListRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_members_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.teamID = getIntent().getStringExtra(IntentExtrasTags.TEAM_ID);
        this.loaderDialog = new LoaderDialog(this);
        this.bindViewsToObjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.athletesListRV.setAdapter(null);
        this.coachesListRV.setAdapter(null);
        SharedPreferences sharedPrefs = getSharedPreferences(Constants.SHARED_PREFERENCES_STRING, MODE_PRIVATE);
        this.loggedUsername = sharedPrefs.getString(IntentExtrasTags.LOGGED_USER, null);
        if (this.loggedUsername != null) {
            this.loaderDialog.show(getString(R.string.members_load_msg));
            this.completedRecyclerViews = 0;
            this.teamFetchListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(isFinishing() || isDestroyed()){
                        return;
                    }
                    if (snapshot.exists()) {
                        String updatedTeamID = snapshot.child(KeyStrings.TEAM).getValue(String.class);
                        if (updatedTeamID != null) {
                            MembersListActivity.this.teamID = updatedTeamID;
                        }
                        MembersListActivity.this.fetchUsersFromDB(MembersListActivity.this.coachesListRV, Constants.COACH_ROLE_STRING);
                        MembersListActivity.this.fetchUsersFromDB(MembersListActivity.this.athletesListRV, Constants.PLAYER_ROLE_STRING);
                    } else {
                        MembersListActivity.this.loaderDialog.hide();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if(!isFinishing() && !isDestroyed()){
                        MembersListActivity.this.loaderDialog.hide();
                        Toast.makeText(MembersListActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
                    }
                }
            };
            this.dbRef.child(ReferenceStrings.USERS).child(this.loggedUsername).addListenerForSingleValueEvent(this.teamFetchListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        /* Cleanup event listeners to avoid memory leaks */
        if(this.teamFetchListener != null){
            this.dbRef.child(ReferenceStrings.USERS).child(this.loggedUsername).removeEventListener(this.teamFetchListener);
            this.teamFetchListener = null;
        }
        if(this.teamID != null) {
            Query teamMembersQuery = this.dbRef.child(ReferenceStrings.USERS).orderByChild(KeyStrings.TEAM).equalTo(this.teamID);
            if (this.athletesFetchListener != null) {
                teamMembersQuery.removeEventListener(this.athletesFetchListener);
                this.athletesFetchListener = null;
            }
            if (this.coachesFetchListener != null) {
                teamMembersQuery.removeEventListener(this.coachesFetchListener);
                this.coachesFetchListener = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(this.loaderDialog != null){
            this.loaderDialog.hide();
        }
    }

    /**
     * Given the respective XML file, this method binds each view to its corresponding Java object.
     * <br>In this specific case, this method will also define the layout manager for the RecyclerView as well as the respective adapter
     */
    private void bindViewsToObjects(){
        this.coachesListRV = findViewById(R.id.coachesListRV);
        this.coachesListRV.setLayoutManager(new LinearLayoutManager(this));
        this.athletesListRV = findViewById(R.id.athletesListRV);
        this.athletesListRV.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Fetches the list of users from the Firebase DB in order to populate the lists
     * @param targetRecyclerView the RecyclerView into which the results must be put
     * @param role represents the type of users we want to fetch from the DB
     */
    private void fetchUsersFromDB(RecyclerView targetRecyclerView, String role){
        if(this.teamID == null){
            this.completedRecyclerViews++;
            this.checkLoaderDismissal();
            return;
        }
        ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        Query q = this.dbRef.child(ReferenceStrings.USERS).orderByChild(KeyStrings.TEAM).equalTo(this.teamID);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                userList.clear();
                MembersListActivity.this.completedRecyclerViews++;
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        /* Only fetch users based on the desired role */
                        if (role.equals(userSnapshot.child(KeyStrings.ROLE).getValue(String.class))) {
                            HashMap<String, String> user = new HashMap<>();
                            user.put(KeyStrings.USERNAME, userSnapshot.getKey());
                            user.put(KeyStrings.NAME, userSnapshot.child(KeyStrings.NAME).getValue(String.class));
                            user.put(KeyStrings.SURNAME, userSnapshot.child(KeyStrings.SURNAME).getValue(String.class));
                            user.put(KeyStrings.BIRTHDATE, userSnapshot.child(KeyStrings.BIRTHDATE).getValue(String.class));
                            user.put(KeyStrings.PROFILE_PIC, userSnapshot.child(KeyStrings.PROFILE_PIC).getValue(String.class));
                            user.put(KeyStrings.ROLE, userSnapshot.child(KeyStrings.ROLE).getValue(String.class));
                            userList.add(user);
                        }
                    }
                    MemberListAdapter usersAdapter = new MemberListAdapter(userList);
                    targetRecyclerView.setAdapter(usersAdapter);
                }
                MembersListActivity.this.checkLoaderDismissal();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinishing() && !isDestroyed()) {
                    MembersListActivity.this.completedRecyclerViews++;
                    MembersListActivity.this.checkLoaderDismissal();
                    Toast.makeText(MembersListActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
                }
            }
        };
        if(role.equals(Constants.COACH_ROLE_STRING)){
            this.coachesFetchListener = listener;
        } else {
            this.athletesFetchListener = listener;
        }
        q.addValueEventListener(listener);
    }

    /**
     * Dismisses the loader dialog only after all the members are fetched
     */
    private void checkLoaderDismissal(){
        if(this.completedRecyclerViews == 2){
            this.loaderDialog.hide();
            boolean athletesEmpty = (this.athletesListRV.getAdapter() == null || this.athletesListRV.getAdapter().getItemCount() == 0);
            boolean coachesEmpty = (this.coachesListRV.getAdapter() == null || this.coachesListRV.getAdapter().getItemCount() == 0);
            if(athletesEmpty && coachesEmpty){
                Toast.makeText(this, R.string.no_users_found, Toast.LENGTH_SHORT).show();
            }
        }
    }
}