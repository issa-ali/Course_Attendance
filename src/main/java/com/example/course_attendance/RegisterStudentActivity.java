package com.example.course_attendance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.ContentValues; // Add this import

import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
// The rest of your imports...


public class RegisterStudentActivity extends AppCompatActivity {
    private EditText inputName, inputID;
    private Button btnTakeSelfie, btnDoneStudent;
    private ImageView capturedImageView;
    private String capturedImagePath = "";
    private MediaPlayer clickSound;
    private String currentPhotoPath = "";

    // Declare MediaPlayer for sound effect

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_student);

        // Initialize views
        inputName = findViewById(R.id.inputName);
        inputID = findViewById(R.id.inputID);
        btnTakeSelfie = findViewById(R.id.btnTakeSelfie);
        btnDoneStudent = findViewById(R.id.btnDoneStudent);
        capturedImageView = findViewById(R.id.capturedImageView2);

        // Initialize MediaPlayer with sound resource
        clickSound = MediaPlayer.create(this, R.raw.click_sound);  // Assuming your sound is in res/raw/click_sound.wav

        // Request permissions at runtime (if needed)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        }

        // Set up button click listeners with sound effect
        btnTakeSelfie.setOnClickListener(v -> {
            playClickSound();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent(); // This method is added below
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        });


        btnDoneStudent.setOnClickListener(v -> {
            playClickSound();  // Play sound effect on button click
            registerStudent();
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
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

    private void playClickSound() {
        if (clickSound != null) {
            clickSound.start();  // Play the sound effect
        }
    }

    private void registerStudent() {
        String name = inputName.getText().toString().trim();
        String id = inputID.getText().toString().trim();

        if (name.isEmpty() || id.isEmpty()) {
            Toast.makeText(this, "Please enter name and ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capturedImagePath.isEmpty()) {
            Toast.makeText(this, "Please take a selfie", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Convert image to Base64 (after compression)
            FileInputStream fis = openFileInput(capturedImagePath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            fis.close();
            byte[] imageBytes = baos.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            Bitmap compressedBitmap = compressImage(bitmap);
            byte[] compressedBytes = convertBitmapToBytes(compressedBitmap);
            String base64Image = Base64.encodeToString(compressedBytes, Base64.NO_WRAP);

            // Create JSON payload
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("id", id);
            json.put("selfie", base64Image);

            // Send to server
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url("http://192.168.192.39:5000/register") // Use the correct server URL
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(RegisterStudentActivity.this, "Failed to connect to server: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterStudentActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(RegisterStudentActivity.this, "Registration failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing registration: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String saveBitmapToInternalStorage(Bitmap bitmap) {
        String filename = "selfie_" + System.currentTimeMillis() + ".jpg";
        try {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filename;
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

    private Bitmap compressImage(Bitmap original) {
        // Compress the image to reduce its size
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        original.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);  // 50% quality
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    private byte[] convertBitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Load the high-quality image from file
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            capturedImageView.setImageBitmap(bitmap);
            capturedImagePath = saveBitmapToInternalStorage(bitmap);
            Toast.makeText(this, "Selfie saved", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
