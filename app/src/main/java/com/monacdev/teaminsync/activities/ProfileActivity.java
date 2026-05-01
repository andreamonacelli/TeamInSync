package com.monacdev.teaminsync.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.fragments.RegistrationWizardFragment;
import com.monacdev.teaminsync.constants.Constants;
import com.monacdev.teaminsync.loaders.LoaderDialog;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {
    private String displayedUserID;
    private String displayedUserSurname;
    private LoaderDialog loaderDialog;
    private final HashMap<String, String> editData = new HashMap<>();
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private TextView profileNameHeaderTV;
    private ImageView profilePictureIV;
    private TextView roleLabelTV;
    private TextView birthDateLabelTV;
    private TextView userUsernameTV;
    private Button toMyTrainingsPageBtn;
    private Button toSquadListBtn;
    private ImageButton editProfileBtn;

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

        this.displayedUserID = getIntent().getStringExtra(IntentExtrasTags.DISPLAYED_USER);
        getSupportFragmentManager().setFragmentResultListener(NavigationTags.EDIT_FRAGMENT_RESULT, this, (requestKey, result) -> {
            ProfileActivity.this.fillUserDataFromDB();
            Log.i("user_profile_updated", "User profile has been updated and reloaded");
        }
        );
        this.loaderDialog = new LoaderDialog(this);
        this.bindViewsToObjects();
        this.fillUserDataFromDB();
        this.setListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(this.loaderDialog != null){
            this.loaderDialog.hide();
        }
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
        this.editProfileBtn = findViewById(R.id.editProfileBtn);
    }

    /**
     * Defines the listeners for the View components within the current activity
     */
    private void setListeners(){
        this.toSquadListBtn.setOnClickListener(view -> finish());
        this.toMyTrainingsPageBtn.setOnClickListener(view -> {
            Intent trainingsIntent = new Intent(ProfileActivity.this, TrainingActivity.class);
            trainingsIntent.putExtra(IntentExtrasTags.DISPLAYED_USER, displayedUserID);
            trainingsIntent.putExtra(IntentExtrasTags.DISPLAYED_USER_SURNAME, displayedUserSurname);
            startActivity(trainingsIntent);
        });
        this.editProfileBtn.setOnClickListener(view -> {
            RegistrationWizardFragment editWizard = RegistrationWizardFragment.newEditingInstance(ProfileActivity.this.editData);
            editWizard.show(getSupportFragmentManager(), NavigationTags.EDIT_WIZARD);
        });
    }

    /**
     * Given the user that is displayed in the page, fetch its data from the Firebase DB
     */
    private void fillUserDataFromDB(){
        if(this.displayedUserID == null || this.displayedUserID.isEmpty()){
            Toast.makeText(this, R.string.error_invalid_user, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        DatabaseReference userRef = this.dbRef.child(ReferenceStrings.USERS).child(this.displayedUserID);
        this.loaderDialog.show(getString(R.string.loading_user_msg));
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                ProfileActivity.this.loaderDialog.hide();
                if(snapshot.exists()){
                    String name = snapshot.child(KeyStrings.NAME).getValue(String.class);
                    String surname = snapshot.child(KeyStrings.SURNAME).getValue(String.class);
                    ProfileActivity.this.displayedUserSurname = surname;
                    if(name != null && surname != null){
                        ProfileActivity.this.profileNameHeaderTV.setText(String.format("%s %s", name, surname));
                    }
                    String role = snapshot.child(KeyStrings.ROLE).getValue(String.class);
                    if(role != null){
                        if(role.equals(Constants.COACH_ROLE_STRING)){
                            ProfileActivity.this.roleLabelTV.setText(R.string.coach_label);
                        } else {
                            ProfileActivity.this.roleLabelTV.setText(R.string.athlete_label);
                        }
                    }
                    String birthDate = snapshot.child(KeyStrings.BIRTHDATE).getValue(String.class);
                    ProfileActivity.this.birthDateLabelTV.setText(String.format("%s %s", getString(R.string.born_on_label), birthDate));
                    ProfileActivity.this.userUsernameTV.setText(String.format("@%s", snapshot.getKey()));
                    String logoPath = snapshot.child(KeyStrings.PROFILE_PIC).getValue(String.class);
                    ProfileActivity.this.setProfilePicture(logoPath, role);
                    ProfileActivity.this.populateEditData(snapshot);
                    ProfileActivity.this.showingLoggedUser(snapshot.child(KeyStrings.EMAIL).getValue(String.class));
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

    /**
     * Given the currently logged user's email, checks whether we are visiting its own profile page
     * and consequently shows/hides the edit profile button
     * @param displayedUserEmail the email of the currently logged user
     */
    private void showingLoggedUser(String displayedUserEmail){
        FirebaseAuth authClient = FirebaseAuth.getInstance();
        FirebaseUser loggedUser = authClient.getCurrentUser();
        if(loggedUser != null){
            String loggedUserEmail = loggedUser.getEmail();
            if(loggedUserEmail != null && loggedUserEmail.equals(displayedUserEmail)){
                this.editProfileBtn.setVisibility(View.VISIBLE);
            } else {
                this.editProfileBtn.setVisibility(View.GONE);
            }
        } else {
            this.editProfileBtn.setVisibility(View.GONE);
        }
    }

    /**
     * Given the DataSnapshot for the user, populates the map that should eventually be given to the edit page
     * @param snapshot the DataSnapshot for the displayed user
     */
    private void populateEditData(DataSnapshot snapshot){
        String name = snapshot.child(KeyStrings.NAME).getValue(String.class);
        String surname = snapshot.child(KeyStrings.SURNAME).getValue(String.class);
        String birthDate = snapshot.child(KeyStrings.BIRTHDATE).getValue(String.class);
        String logoPath = snapshot.child(KeyStrings.PROFILE_PIC).getValue(String.class);
        String team = snapshot.child(KeyStrings.TEAM).getValue(String.class);
        ProfileActivity.this.displayedUserSurname = surname;
        ProfileActivity.this.editData.put(KeyStrings.USERNAME, ProfileActivity.this.displayedUserID);
        ProfileActivity.this.editData.put(KeyStrings.NAME, name);
        ProfileActivity.this.editData.put(KeyStrings.SURNAME, surname);
        ProfileActivity.this.editData.put(KeyStrings.BIRTHDATE, birthDate);
        ProfileActivity.this.editData.put(KeyStrings.PROFILE_PIC, logoPath);
        ProfileActivity.this.editData.put(KeyStrings.TEAM, team);
    }
}