package com.monacdev.teaminsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private ImageView teamLogoIV;
    private TextView homepageHeaderTV;
    private Button toTrainingPageBtn;
    private Button squadListBtn;
    private TextView teamNameTV;
    private TextView leagueNameTV;
    private TextView cityStadiumTV;
    private ImageButton openNotificationsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.bindViewsWithObjects();

        /* Defining Button Listeners */
        this.squadListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent squadListIntent = new Intent(MainActivity.this, MembersListActivity.class);
                startActivity(squadListIntent);
            }
        });
        this.toTrainingPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent trainingPageIntent = new Intent(MainActivity.this, TrainingActivity.class);
                startActivity(trainingPageIntent);
            }
        });
        this.openNotificationsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                * TODO: implement a Fragment to show notifications
                */
            }
        });
    }

    /**
     * Binds the Views defined within the XML layout file for the activity to their respective Java objects
     */
    private void bindViewsWithObjects(){
        this.teamLogoIV = findViewById(R.id.teamLogoIV);
        this.homepageHeaderTV = findViewById(R.id.homepageHeaderTV);
        this.toTrainingPageBtn = findViewById(R.id.toTrainingPageBtn);
        this.squadListBtn = findViewById(R.id.squadListBtn);
        this.teamNameTV = findViewById(R.id.teamNameTV);
        this.leagueNameTV = findViewById(R.id.leagueNameTV);
        this.cityStadiumTV = findViewById(R.id.cityStadiumTV);
        this.openNotificationsBtn = findViewById(R.id.openNotificationsBtn);
    }
}