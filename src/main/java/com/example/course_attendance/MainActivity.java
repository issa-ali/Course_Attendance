package com.example.course_attendance;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    Button btnRegister, btnLogin;
    MediaPlayer clickSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the status bar (method 1)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);

        clickSound = MediaPlayer.create(this, R.raw.click_sound);

        btnRegister.setOnClickListener(v -> {
            playClickSound();
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            playClickSound();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Additional hiding for when user returns to activity
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void playClickSound() {
        if (clickSound != null) {
            clickSound.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
    }
}