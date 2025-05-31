package com.example.course_attendance;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FaceRecognitionActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private ImageView capturedImageView;
    private Button btnCaptureSelfie, btnSubmitAttendance;
    private String currentPhotoPath = "";
    private String currentCourseName;
    private int currentCourseIndex;
    private MediaPlayer clickSoundPlayer;
    private MediaPlayer popupSoundPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        // Initialize sound players
        clickSoundPlayer = MediaPlayer.create(this, R.raw.click_sound);
        popupSoundPlayer = MediaPlayer.create(this, R.raw.pop_up);

        currentCourseIndex = getIntent().getIntExtra("courseIndex", -1);
        currentCourseName = getIntent().getStringExtra("courseName");
        capturedImageView = findViewById(R.id.capturedImageView);
        btnCaptureSelfie = findViewById(R.id.btnCaptureSelfie);
        btnSubmitAttendance = findViewById(R.id.btnSubmitAttendance);

        // Request camera permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION);
        }

        btnCaptureSelfie.setOnClickListener(v -> {
            playClickSound();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                showAttendanceDialog(false, "Permission Required", "", "", "Camera permission is needed to take photos");
            }
        });

        btnSubmitAttendance.setOnClickListener(v -> {
            playClickSound();
            if (currentPhotoPath.isEmpty()) {
                showAttendanceDialog(false, "No Photo", "", "", "Please capture an image first");
                return;
            }
            verifyAttendance();
        });
    }

    private void playClickSound() {
        if (clickSoundPlayer != null) {
            clickSoundPlayer.seekTo(0); // Rewind to start if already playing
            clickSoundPlayer.start();
        }
    }

    private void playPopupSound() {
        if (popupSoundPlayer != null) {
            popupSoundPlayer.seekTo(0); // Rewind to start if already playing
            popupSoundPlayer.start();
        }
    }

    private void showAttendanceDialog(boolean isSuccess, String title, String studentName, String studentId, String courseOrMessage) {
        // Play popup sound when dialog appears
        playPopupSound();

        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_attendance_result, null);

        // Initialize views
        ImageView ivIcon = dialogView.findViewById(R.id.ivDialogIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvStudentName = dialogView.findViewById(R.id.tvStudentName);
        TextView tvStudentId = dialogView.findViewById(R.id.tvStudentId);
        TextView tvCourseName = dialogView.findViewById(R.id.tvCourseName);
        View dialogContainer = dialogView.findViewById(R.id.dialogContainer);

        // Set dialog content
        if (isSuccess) {
            ivIcon.setImageResource(R.drawable.ic_success);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.green));
            tvStudentName.setText(String.format("Student: %s", studentName));
            tvStudentId.setText(String.format("ID: %s", studentId));
            tvCourseName.setText(String.format("Course: %s", courseOrMessage));
        } else {
            ivIcon.setImageResource(R.drawable.ic_error);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.red));
            tvStudentName.setText("");  // Hide student name for errors
            tvStudentId.setText("");    // Hide student ID for errors
            tvCourseName.setText(courseOrMessage); // Show error message here
        }

        tvTitle.setText(title);

        // Create the dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        // Dismiss when clicked
        dialogContainer.setOnClickListener(v -> dialog.dismiss());

        // Configure window
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set 50% width
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int width = (int)(metrics.widthPixels * 0.5);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Center the dialog
            window.setGravity(Gravity.CENTER);
        }

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release media players when activity is destroyed
        if (clickSoundPlayer != null) {
            clickSoundPlayer.release();
            clickSoundPlayer = null;
        }
        if (popupSoundPlayer != null) {
            popupSoundPlayer.release();
            popupSoundPlayer = null;
        }
    }

    // ... [rest of your existing methods remain unchanged] ...
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                showAttendanceDialog(false, "Error", "", "", "Error creating image file");
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Load the high-quality image from file
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            capturedImageView.setImageBitmap(bitmap);
        } else if (resultCode == RESULT_CANCELED) {
            showAttendanceDialog(false, "Cancelled", "", "", "Photo capture was cancelled");
        }
    }

    private void verifyAttendance() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        if (bitmap == null) {
            showAttendanceDialog(false, "Error", "", "", "Error loading captured image");
            return;
        }

        // Compress the image before sending
        Bitmap compressedBitmap = compressImage(bitmap);
        String base64Image = encodeImageToBase64(compressedBitmap);
        sendToServerForVerification(base64Image, currentCourseName, currentCourseIndex);
    }

    private Bitmap compressImage(Bitmap original) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        original.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void sendToServerForVerification(String base64Image, String courseName, int courseIndex) {
        OkHttpClient client = new OkHttpClient();

        JSONObject json = new JSONObject();
        try {
            json.put("image", base64Image);
            json.put("courseName", courseName);
            json.put("courseIndex", courseIndex);
        } catch (JSONException e) {
            e.printStackTrace();
            showAttendanceDialog(false, "Error", "", "", "Error preparing data");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(),
                MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("http://192.168.192.39:5000/recognize")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        showAttendanceDialog(false, "Connection Error", "", "", "Failed to connect to server"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        if (jsonResponse.has("error")) {
                            String error = jsonResponse.getString("error");
                            showAttendanceDialog(false, "Verification Failed", "", "", error);
                        } else if (jsonResponse.has("message")) {
                            String name = jsonResponse.getString("name");
                            String studentId = jsonResponse.optString("id", "N/A");
                            String course = jsonResponse.optString("course", currentCourseName);
                            showAttendanceDialog(true, "Attendance Confirmed",
                                    name, studentId, course);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showAttendanceDialog(false, "Error", "", "", "Error processing server response");
                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, no need to show dialog
            } else {
                showAttendanceDialog(false, "Permission Denied", "", "", "Camera permission is required");
            }
        }
    }
}