package com.monacdev.teaminsync;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.utils.Constants;

public class TrainingActivity extends AppCompatActivity {
    private TextView trainingPageHeaderTV;
    private RecyclerView trainingListRV;
    /** This will be either a training-creation wizard or a training-tracking fragment based on the logged user's role */
    private Fragment trainingFragment;
    private ImageButton createTrainingBtn;
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private String displayedUserID;
    private boolean isMyTrainingList;
    private String loggedUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_training);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.displayedUserID = getIntent().getStringExtra(Constants.DISPLAYED_USER_EXTRA_STRING);
        this.bindViewsToObjects();
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     */
    private void bindViewsToObjects(){
        this.trainingPageHeaderTV = findViewById(R.id.trainingPageHeaderTV);
        this.trainingListRV = findViewById(R.id.trainingListRV);
        this.createTrainingBtn = findViewById(R.id.createTrainingBtn);
    }

    /**
     * Checks if the current training page corresponds to the one of the currently logged user and adjusts the UI accordingly
     */
    private void isDisplayedUserLogged(){
        FirebaseAuth authClient = FirebaseAuth.getInstance();
        FirebaseUser loggedUser = authClient.getCurrentUser();
        if(loggedUser != null){
            String loggedUserEmail = loggedUser.getEmail();
            DatabaseReference displayedUserRef = this.dbRef.child(Constants.USERS_REFERENCE_STRING).child(this.displayedUserID);
            displayedUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        String displayedUserEmail = snapshot.child(Constants.EMAIL_KEY_STRING).getValue(String.class);
                        String displayedUserRole = snapshot.child(Constants.ROLE_KEY_STRING).getValue(String.class);
                        if(loggedUserEmail != null && loggedUserEmail.equals(displayedUserEmail)){
                            isMyTrainingList = true;
                            setAddTrainingBtnVisibility(displayedUserRole);
                        } else {
                            isMyTrainingList = false;
                        }
                    } else {
                        isMyTrainingList = false;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TrainingActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
                    isMyTrainingList = false;
                }
            });
        } else {
            isMyTrainingList = false;
        }
    }

    /**
     * If the displayed user is the currently logged one and the user is a coach then display the "Add Training Button"
     */
    private void setAddTrainingBtnVisibility(String role){
        if(role != null && role.equals(Constants.COACH_ROLE_STRING)){
            this.createTrainingBtn.setVisibility(View.VISIBLE);
        } else {
            this.createTrainingBtn.setVisibility(View.GONE);
        }
    }
}