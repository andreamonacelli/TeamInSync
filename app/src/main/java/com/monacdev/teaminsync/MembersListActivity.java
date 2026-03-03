package com.monacdev.teaminsync;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MembersListActivity extends AppCompatActivity {
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
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

        this.bindViewsToObjects();
    }

    /**
     * Given the respective XML file, this method binds each view to its corresponding Java object
     */
    private void bindViewsToObjects(){
        this.coachesListRV = findViewById(R.id.coachesListRV);
        this.athletesListRV = findViewById(R.id.athletesListRV);
    }

    /**
     * Fetches the list of users from the Firebase DB in order to populate the lists
     */
    private void fetchDataFromDB(){

    }
}