package com.example.shino.FireApp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button mFighter, mCitizen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mFighter= (Button)findViewById(R.id.fighter);
        mCitizen=(Button)findViewById(R.id.citizen);

        mFighter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,FighterLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mCitizen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,CitizenLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }
}
