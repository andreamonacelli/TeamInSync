package com.monacdev.teaminsync;

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
import com.monacdev.teaminsync.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class MembersListActivity extends AppCompatActivity {
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private String teamID;
    private RecyclerView coachesListRV;
    //private MemberListAdapter coachesListAdapter;
    private RecyclerView athletesListRV;
    //private MemberListAdapter athletesListAdapter;

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

        this.teamID = getIntent().getStringExtra(Constants.TEAM_ID_TAG);
        this.bindViewsToObjects();
    }

    /**
     * Given the respective XML file, this method binds each view to its corresponding Java object.
     * <br>In this specific case, this method will also define the layout manager for the RecyclerView as well as the respective adapter
     */
    private void bindViewsToObjects(){
        this.coachesListRV = findViewById(R.id.coachesListRV);
        this.coachesListRV.setLayoutManager(new LinearLayoutManager(this));
        this.fetchUsersFromDB(this.coachesListRV, Constants.COACH_ROLE_STRING);
        this.athletesListRV = findViewById(R.id.athletesListRV);
        this.athletesListRV.setLayoutManager(new LinearLayoutManager(this));
        this.fetchUsersFromDB(this.athletesListRV, Constants.PLAYER_ROLE_STRING);
    }

    /**
     * Fetches the list of users from the Firebase DB in order to populate the lists
     * @param role represents the type of users we want to fetch from the DB
     */
    private void fetchUsersFromDB(RecyclerView targetRecyclerView, String role){
        ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        Query q = this.dbRef.child(Constants.USERS_REFERENCE_STRING).orderByChild(Constants.TEAM_KEY_STRING).equalTo(this.teamID);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                if(snapshot.exists()){
                    for(DataSnapshot userSnapshot : snapshot.getChildren()){
                        /* Only fetch users based on the desired role */
                        if(role.equals(userSnapshot.child(Constants.ROLE_KEY_STRING).getValue(String.class))){
                            HashMap<String, String> user = new HashMap<>();
                            user.put(Constants.USERNAME_KEY_STRING, userSnapshot.getKey());
                            user.put(Constants.NAME_KEY_STRING, userSnapshot.child(Constants.NAME_KEY_STRING).getValue(String.class));
                            user.put(Constants.SURNAME_KEY_STRING, userSnapshot.child(Constants.SURNAME_KEY_STRING).getValue(String.class));
                            user.put(Constants.BIRTHDATE_KEY_STRING, userSnapshot.child(Constants.BIRTHDATE_KEY_STRING).getValue(String.class));
                            user.put(Constants.PROFILE_PIC_KEY_STRING, userSnapshot.child(Constants.PROFILE_PIC_KEY_STRING).getValue(String.class));
                            user.put(Constants.ROLE_KEY_STRING, userSnapshot.child(Constants.ROLE_KEY_STRING).getValue(String.class));
                            userList.add(user);
                        }
                    }
                    MemberListAdapter usersAdapter = new MemberListAdapter(userList);
                    targetRecyclerView.setAdapter(usersAdapter);
                } else {
                    Toast.makeText(MembersListActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MembersListActivity.this, R.string.no_users_found, Toast.LENGTH_SHORT).show();
            }
        });
    }
}