package com.example.course_attendance;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    private ImageButton btnRegisterStudent, btnRegisterCourse;
    private MediaPlayer clickSound;  // Declare MediaPlayer to play the sound

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize ImageButtons from the layout
        btnRegisterStudent = findViewById(R.id.btnRegisterStudent);
        btnRegisterCourse = findViewById(R.id.btnRegisterCourse);

        // Initialize MediaPlayer with sound resource
        clickSound = MediaPlayer.create(this, R.raw.click_sound);  // Assuming your sound is in res/raw/click_sound.wav

        // Set OnClickListener for the Register Student button
        if (btnRegisterStudent != null) {
            btnRegisterStudent.setOnClickListener(v -> {
                playClickSound();  // Play sound effect
                startActivity(new Intent(RegisterActivity.this, RegisterStudentActivity.class));  // Navigate to RegisterStudentActivity
            });
        }

        // Set OnClickListener for the Register Course button
        if (btnRegisterCourse != null) {
            btnRegisterCourse.setOnClickListener(v -> {
                playClickSound();  // Play sound effect
                startActivity(new Intent(RegisterActivity.this, RegisterCourseActivity.class));  // Navigate to RegisterCourseActivity
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Maintain fullscreen when returning to activity
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    // Method to play the click sound
    private void playClickSound() {
        if (clickSound != null) {
            clickSound.start();  // Play the sound
        }
    }

    // Release the MediaPlayer when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
    }
}
