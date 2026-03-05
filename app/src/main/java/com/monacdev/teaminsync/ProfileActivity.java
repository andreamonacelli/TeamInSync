package com.monacdev.teaminsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.utils.Constants;

public class ProfileActivity extends AppCompatActivity {
    private String displayedUserID;
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private TextView profileNameHeaderTV;
    private ImageView profilePictureIV;
    private TextView roleLabelTV;
    private TextView birthDateLabelTV;
    private TextView userUsernameTV;
    private Button toMyTrainingsPageBtn;
    private Button toSquadListBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.displayedUserID = getIntent().getStringExtra(Constants.DISPLAYED_USER_EXTRA_STRING);
        this.bindViewsToObjects();
        this.fillUserDataFromDB();

        /* Defining the button listeners */
        this.toSquadListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* We can safely assume that we got here from either the squad list page */
                finish();
            }
        });
        this.toMyTrainingsPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Here we are navigating to the page containing the training of the displayed user which might not be the logged one! */
                Intent trainingsIntent = new Intent(ProfileActivity.this, TrainingActivity.class);
                trainingsIntent.putExtra(Constants.DISPLAYED_USER_EXTRA_STRING, displayedUserID);
                startActivity(trainingsIntent);
            }
        });
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     */
    private void bindViewsToObjects(){
        this.profileNameHeaderTV = findViewById(R.id.profileNameHeaderTV);
        this.profilePictureIV = findViewById(R.id.profilePictureIV);
        this.roleLabelTV = findViewById(R.id.roleLabelTV);
        this.birthDateLabelTV = findViewById(R.id.birthDateLabelTV);
        this.userUsernameTV = findViewById(R.id.userUsernameTV);
        this.toMyTrainingsPageBtn = findViewById(R.id.toMyTrainingsPageBtn);
        this.toSquadListBtn = findViewById(R.id.toSquadListBtn);
    }

    /**
     * Given the user that is displayed in the page, fetch its data from the Firebase DB
     */
    private void fillUserDataFromDB(){
        DatabaseReference userRef = this.dbRef.child(Constants.USERS_REFERENCE_STRING).child(this.displayedUserID);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String name = snapshot.child(Constants.NAME_KEY_STRING).getValue(String.class);
                    String surname = snapshot.child(Constants.SURNAME_KEY_STRING).getValue(String.class);
                    if(name != null && surname != null){
                        profileNameHeaderTV.setText(String.format("%s %s", name, surname));
                    }
                    String role = snapshot.child(Constants.ROLE_KEY_STRING).getValue(String.class);
                    if(role != null){
                        if(role.equals(Constants.COACH_ROLE_STRING)){
                            roleLabelTV.setText(R.string.coach_label);
                        } else {
                            roleLabelTV.setText(R.string.athlete_label);
                        }
                    }
                    String birthDate = snapshot.child(Constants.BIRTHDATE_KEY_STRING).getValue(String.class);
                    birthDateLabelTV.setText(String.format("%s %s", getString(R.string.born_on_label), birthDate));
                    userUsernameTV.setText(String.format("@%s", snapshot.getKey()));
                    String logoPath = snapshot.child(Constants.PROFILE_PIC_KEY_STRING).getValue(String.class);
                    setProfilePicture(logoPath, role);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Given the path of the image, displays the image in the respective ImageView
     * @param logoPath the path of the remote image
     * @param role the role to manage eventual fallback
     */
    private void setProfilePicture(String logoPath, String role){
        if(logoPath != null && !logoPath.isEmpty()){
            Glide.with(ProfileActivity.this).load(logoPath).circleCrop().into(this.profilePictureIV);
        } else {
            if(role != null && role.equals(Constants.COACH_ROLE_STRING)){
                this.profilePictureIV.setImageResource(R.drawable.ic_coach);
            } else {
                this.profilePictureIV.setImageResource(R.drawable.ic_athlete);
            }
        }
    }
}