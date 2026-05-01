package com.monacdev.teaminsync.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.R;
import com.monacdev.teaminsync.constants.KeyStrings;
import com.monacdev.teaminsync.constants.ReferenceStrings;
import com.monacdev.teaminsync.constants.IntentExtrasTags;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.fragments.RegistrationWizardFragment;
import com.monacdev.teaminsync.constants.Constants;
import com.monacdev.teaminsync.loaders.LoaderDialog;
import com.onesignal.OneSignal;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInputET;
    private EditText passwordET;
    private Button signInBtn;
    private Button registerBtn;
    private LoaderDialog loaderDialog;
    private final FirebaseAuth authClient = FirebaseAuth.getInstance();
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private Query firebasePendingQuery;
    private ValueEventListener firebasePendingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.loaderDialog = new LoaderDialog(this);
        this.bindViewsToObjects();
        this.setListeners();

        /* Consistency check to deal with eventual partially completed authentication process interrupted by screen rotation */
        FirebaseUser loggedUser = this.authClient.getCurrentUser();
        if(loggedUser != null){
            this.loaderDialog.show(getString(R.string.sign_in_load_msg));
            this.navigateToNextActivity(loggedUser);
        }
    }

    @Override
    protected void onDestroy() {
        if(this.firebasePendingQuery != null && this.firebasePendingListener != null){
            this.firebasePendingQuery.removeEventListener(this.firebasePendingListener);
        }
        if(this.loaderDialog != null){
            this.loaderDialog.hide();
        }
        super.onDestroy();
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     */
    private void bindViewsToObjects(){
        this.emailInputET = findViewById(R.id.emailInputET);
        this.passwordET = findViewById(R.id.passwordET);
        this.signInBtn = findViewById(R.id.signInBtn);
        this.registerBtn = findViewById(R.id.registerBtn);
    }

    /**
     * Defines the listeners for the View components within the current activity
     */
    private void setListeners(){
        this.signInBtn.setOnClickListener(view -> {
            if(emailInputET.getText().toString().isEmpty() || passwordET.getText().toString().isEmpty()){
                Toast.makeText(LoginActivity.this, R.string.form_not_compiled, Toast.LENGTH_SHORT).show();
            } else {
                this.userSignIn();
            }
        });
        this.registerBtn.setOnClickListener(view -> {
            if(emailInputET.getText().toString().isEmpty() || passwordET.getText().toString().isEmpty()){
                Toast.makeText(LoginActivity.this, R.string.form_not_compiled, Toast.LENGTH_SHORT).show();
            } else {
                this.registerUser();
            }
        });
    }

    /**
     * Manages Sign-In procedure leveraging Firebase API
     */
    private void userSignIn(){
        String email = this.emailInputET.getText().toString();
        String password = this.passwordET.getText().toString();
        this.loaderDialog.show(getString(R.string.sign_in_load_msg));
        this.authClient.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                FirebaseUser loggedUser = authClient.getCurrentUser();
                Toast.makeText(LoginActivity.this, R.string.auth_ok, Toast.LENGTH_SHORT).show();
                if(loggedUser != null){
                    navigateToNextActivity(loggedUser);
                } else {
                    this.loaderDialog.hide();
                    Toast.makeText(LoginActivity.this, R.string.auth_session_creation_err, Toast.LENGTH_SHORT).show();
                }
            } else {
                this.loaderDialog.hide();
                Toast.makeText(LoginActivity.this, R.string.auth_generic_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Manages User Creation procedure leveraging Firebase API
     */
    private void registerUser(){
        /* Check needed to avoid multiple instances of the fragment to be loaded */
        if(getSupportFragmentManager().isStateSaved() || getSupportFragmentManager().findFragmentByTag(NavigationTags.REG_WIZARD) != null){
            return;
        }
        String email = this.emailInputET.getText().toString();
        String password = this.passwordET.getText().toString();
        RegistrationWizardFragment wizard = RegistrationWizardFragment.newInstance(email, password);
        wizard.show(getSupportFragmentManager(), NavigationTags.REG_WIZARD);
    }

    /**
     * Manages user data retrieval from the Firebase DB and switches to the HomePage
     * @param loggedUser instance of the user that just logged in
     */
    private void navigateToNextActivity(FirebaseUser loggedUser){
        String userEmail = loggedUser.getEmail();
        if(userEmail == null){
            this.loaderDialog.hide();
            return;
        }
        this.firebasePendingQuery = this.dbRef.child(ReferenceStrings.USERS).orderByChild(KeyStrings.EMAIL).equalTo(userEmail);
        this.firebasePendingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(isFinishing() || isDestroyed()){
                    return;
                }
                LoginActivity.this.loaderDialog.hide();
                if(snapshot.exists()){
                    for(DataSnapshot userSnapshot : snapshot.getChildren()){
                        String loggedUserID = userSnapshot.getKey();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra(IntentExtrasTags.LOGGED_USER, loggedUserID);
                        LoginActivity.this.saveUserForPersistence(loggedUserID);
                        if(loggedUserID != null) {
                            OneSignal.login(loggedUserID);
                        }
                        startActivity(intent);
                        finish();
                        break;
                    }
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.error_user_not_found, userEmail), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(isFinishing() || isDestroyed()){
                    return;
                }
                LoginActivity.this.loaderDialog.hide();
                Toast.makeText(LoginActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        };
        this.firebasePendingQuery.addListenerForSingleValueEvent(this.firebasePendingListener);
    }

    /**
     * Saves the currently logged user into the SharedPreferences in order to manage persistence
     */
    private void saveUserForPersistence(String username){
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_STRING, MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
        sharedPrefsEditor.putString(IntentExtrasTags.LOGGED_USER, username);
        sharedPrefsEditor.apply();
    }
}