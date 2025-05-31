package com.example.course_attendance;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private GridLayout layoutCourses;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        layoutCourses = findViewById(R.id.layoutCourses);
        displayAvailableCourses();

        // Initialize media player for click sound
        mediaPlayer = MediaPlayer.create(this, R.raw.click_sound); // replace 'click_sound' with your sound file name
    }

    private void displayAvailableCourses() {
        try {
            JSONArray courses = DataManager.getAllCourses(this);

            if (courses.length() == 0) {
                showNoCoursesMessage();
                return;
            }

            for (int i = 0; i < courses.length(); i++) {
                JSONObject course = courses.getJSONObject(i);
                addCourseCard(course, i);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCourseCard(JSONObject course, int courseIndex) {
        String courseName = course.optString("courseName", "Unnamed");
        String professor = course.optString("professor", "Unknown");
        String day = course.optString("day", "N/A");
        String start = course.optString("startTime", "--:--");
        String end = course.optString("endTime", "--:--");

        // Card container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(48, 48, 48, 48);
        card.setBackgroundResource(R.drawable.card_background);
        card.setGravity(Gravity.CENTER);

        // Adjust GridLayout parameters to have 3 columns
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0;
        cardParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Distribute space evenly across columns
        cardParams.setMargins(16, 16, 16, 16);
        card.setLayoutParams(cardParams);

        // Create and style TextViews
        TextView txtCourse = new TextView(this);
        txtCourse.setText("📘 " + courseName);
        txtCourse.setTextSize(45);
        txtCourse.setTypeface(null, Typeface.BOLD);

        TextView txtProf = new TextView(this);
        txtProf.setText("👨‍🏫 " + professor);
        txtProf.setTextSize(40);

        TextView txtDay = new TextView(this);
        txtDay.setText("📅 " + day);
        txtDay.setTextSize(35);

        TextView txtTime = new TextView(this);
        txtTime.setText("⏰ " + start + " - " + end);
        txtTime.setTextSize(35);

        // Add views to card
        card.addView(txtCourse);
        card.addView(txtProf);
        card.addView(txtDay);
        card.addView(txtTime);

        card.setOnClickListener(v -> {
            // Play sound when course is clicked
            playClickSound();
            // Launch face recognition activity
            launchFaceRecognition(courseIndex);
        });

        // Add the card to the GridLayout
        layoutCourses.addView(card);
    }

    private void launchFaceRecognition(int courseIndex) {
        try {
            JSONObject course = DataManager.getAllCourses(this).getJSONObject(courseIndex);
            String courseName = course.getString("courseName");

            Intent intent = new Intent(this, FaceRecognitionActivity.class);
            intent.putExtra("courseIndex", courseIndex);
            intent.putExtra("courseName", courseName); // Add course name to intent
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading course data", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoCoursesMessage() {
        Toast.makeText(this, "No courses available. Please register courses first.", Toast.LENGTH_LONG).show();

        Button btnRegister = new Button(this);
        btnRegister.setText("Register New Course");
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterCourseActivity.class));
            finish();
        });
        layoutCourses.addView(btnRegister);
    }

    private void playClickSound() {
        if (mediaPlayer != null) {
            mediaPlayer.start(); // Play the sound
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        layoutCourses.removeAllViews();
        displayAvailableCourses();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Release media player to free up resources
        }
    }
}
