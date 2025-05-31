package com.example.course_attendance;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterCourseActivity extends AppCompatActivity {
    private EditText courseName, professorName, courseDay, startTime, endTime;
    private Button btnDoneCourse;
    private MediaPlayer clickSound;  // Declare MediaPlayer for sound effect

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_course);

        // Initialize views
        courseName = findViewById(R.id.courseName);
        professorName = findViewById(R.id.professorName);
        courseDay = findViewById(R.id.courseDay);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        btnDoneCourse = findViewById(R.id.btnDoneCourse);

        // Initialize MediaPlayer with sound resource
        clickSound = MediaPlayer.create(this, R.raw.click_sound);  // Assuming your sound is in res/raw/click_sound.wav

        // Set OnClickListener for the Done Course button
        btnDoneCourse.setOnClickListener(v -> {
            playClickSound();  // Play sound effect on button click

            // Validate input fields
            if (validateInputs()) {
                // Save course data
                DataManager.saveCourse(this,
                        courseName.getText().toString().trim(),
                        professorName.getText().toString().trim(),
                        courseDay.getText().toString().trim(),
                        startTime.getText().toString().trim(),
                        endTime.getText().toString().trim());

                Toast.makeText(this, "Course Registered", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // Method to play the click sound
    private void playClickSound() {
        if (clickSound != null) {
            clickSound.start();  // Play the sound effect
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

    @Override
    protected void onResume() {
        super.onResume();
        // Maintain fullscreen when returning to activity
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private boolean validateInputs() {
        if (courseName.getText().toString().trim().isEmpty()) {
            courseName.setError("Course name is required");
            return false;
        }
        if (professorName.getText().toString().trim().isEmpty()) {
            professorName.setError("Professor name is required");
            return false;
        }
        if (courseDay.getText().toString().trim().isEmpty()) {
            courseDay.setError("Course day is required");
            return false;
        }
        if (startTime.getText().toString().trim().isEmpty()) {
            startTime.setError("Start time is required");
            return false;
        }
        if (endTime.getText().toString().trim().isEmpty()) {
            endTime.setError("End time is required");
            return false;
        }
        return true;
    }
}
