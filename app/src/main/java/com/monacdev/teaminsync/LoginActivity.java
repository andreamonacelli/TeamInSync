package com.monacdev.teaminsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monacdev.teaminsync.utils.Constants;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInputET;
    private EditText passwordET;
    private Button signInBtn;
    private Button registerBtn;
    private final FirebaseAuth authClient = FirebaseAuth.getInstance();
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

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

        this.bindViewsToObjects();
        /* Binding listeners to buttons */
        this.signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(emailInputET.getText().toString().isEmpty() || passwordET.getText().toString().isEmpty()){
                    Toast.makeText(LoginActivity.this, R.string.form_not_compiled, Toast.LENGTH_SHORT).show();
                } else {
                    userSignIn();
                }
            }
        });
        this.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(emailInputET.getText().toString().isEmpty() || passwordET.getText().toString().isEmpty()){
                    Toast.makeText(LoginActivity.this, R.string.form_not_compiled, Toast.LENGTH_SHORT).show();
                } else {
                    registerUser();
                }
            }
        });
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
     * Manages Sign-In procedure leveraging Firebase API
     */
    private void userSignIn(){
        String email = this.emailInputET.getText().toString();
        String password = this.passwordET.getText().toString();
        this.authClient.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    FirebaseUser loggedUser = authClient.getCurrentUser();
                    Toast.makeText(LoginActivity.this, R.string.auth_ok, Toast.LENGTH_SHORT).show();
                    if(loggedUser != null){
                        navigateToNextActivity(loggedUser);
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.auth_session_creation_err, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.auth_generic_err, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Manages User Creation procedure leveraging Firebase API
     */
    private void registerUser(){
        String email = this.emailInputET.getText().toString();
        String password = this.passwordET.getText().toString();
        this.authClient.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    FirebaseUser loggedUser = authClient.getCurrentUser();
                    Toast.makeText(LoginActivity.this, R.string.register_ok, Toast.LENGTH_SHORT).show();
                    if(loggedUser != null){
                        RegistrationWizardFragment wizard = RegistrationWizardFragment.newInstance(loggedUser.getEmail());
                        wizard.show(getSupportFragmentManager(), Constants.REG_WIZARD_TAG);
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.auth_session_creation_err, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.register_err, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Manages user data retrieval from the Firebase DB and switches to either the HomePage or the WaitingForApprovalPage
     * @param loggedUser instance of the user that just logged in
     */
    private void navigateToNextActivity(FirebaseUser loggedUser){
        String userEmail = loggedUser.getEmail();
        Query q = this.dbRef.getDatabase().getReference(Constants.USERS_REFERENCE_STRING).orderByChild(Constants.EMAIL_KEY_STRING).equalTo(userEmail);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot userSnapshot : snapshot.getChildren()){
                        String loggedUserID = userSnapshot.getKey();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra(Constants.LOGGED_USER_EXTRA_STRING, loggedUserID);
                        startActivity(intent);
                        finish(); /* Terminates Login Activity */
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "ATTENZIONE: Utente con mail " + userEmail + " non trovato nel sistema!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, R.string.connection_err, Toast.LENGTH_SHORT).show();
            }
        });
    }
}