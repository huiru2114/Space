package com.example.space;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import android.util.Base64;
import com.bumptech.glide.Glide;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profilePicture;
    private Button selectPictureButton;
    private EditText usernameEditText;
    private EditText phoneEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signupButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    private ImageView backButton;
    private ImageView passwordVisibilityToggle;
    private ImageView confirmPasswordVisibilityToggle;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;
    private Uri selectedImageUri = null;
    private String encodedImage = null;

    private SupabaseAuth supabaseAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        supabaseAuth = new SupabaseAuth(this);

        // Use a unique name for signup activity preferences to avoid conflicts
        prefs = getSharedPreferences("SignupPrefs", MODE_PRIVATE);

        // Initialize views
        profilePicture = findViewById(R.id.profile_picture);
        selectPictureButton = findViewById(R.id.btn_select_picture);
        usernameEditText = findViewById(R.id.et_username);
        phoneEditText = findViewById(R.id.et_phone);
        emailEditText = findViewById(R.id.et_email);
        passwordEditText = findViewById(R.id.et_password);
        confirmPasswordEditText = findViewById(R.id.et_confirm_password);
        signupButton = findViewById(R.id.btn_signup);
        loginTextView = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);
        passwordVisibilityToggle = findViewById(R.id.password_visibility_toggle);
        confirmPasswordVisibilityToggle = findViewById(R.id.confirm_password_visibility_toggle);

        // Set listeners
        selectPictureButton.setOnClickListener(v -> openImagePicker());
        signupButton.setOnClickListener(v -> handleSignup());
        loginTextView.setOnClickListener(v -> finish()); // Go back to login
        backButton.setOnClickListener(v -> finish());
        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility(passwordEditText, passwordVisibilityToggle, true));
        confirmPasswordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility(confirmPasswordEditText, confirmPasswordVisibilityToggle, false));
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleIcon, boolean isPassword) {
        if (isPassword) {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleIcon.setImageResource(R.drawable.ic_visibility_on);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleIcon.setImageResource(R.drawable.ic_visibility_off);
            }
        } else {
            confirmPasswordVisible = !confirmPasswordVisible;
            if (confirmPasswordVisible) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleIcon.setImageResource(R.drawable.ic_visibility_on);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleIcon.setImageResource(R.drawable.ic_visibility_off);
            }
        }

        // Move cursor to the end of text
        editText.setSelection(editText.getText().length());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode != RESULT_OK) {
                Log.d(TAG, "Image selection cancelled or failed");
                return;
            }

            if (data == null) {
                Log.e(TAG, "Data is null in onActivityResult");
                Toast.makeText(this, "Failed to get image data", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri imageUri = data.getData();
            if (imageUri == null) {
                Log.e(TAG, "Image URI is null in onActivityResult");
                Toast.makeText(this, "Failed to get image URI", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedImageUri = imageUri;
            try {
                // Use Glide to load and display the image
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop() // This ensures the image is cropped in a circle
                        .override(220, 220) // Set the size to match your ImageView
                        .into(profilePicture);

                // Convert to base64 for Supabase storage
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                if (bitmap != null) {
                    encodedImage = encodeImage(bitmap);
                    Log.d(TAG, "Image encoded successfully");
                } else {
                    Log.e(TAG, "Failed to decode bitmap from URI");
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error processing image: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void handleSignup() {
        String username = usernameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate inputs
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.setError("Please confirm your password");
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Store pending profile info for later use when user confirms email
        // Use user-specific key with email to avoid overwrites
        String userKey = email.replaceAll("[.@]", "_"); // Create a safe key from email
        Log.d(TAG, "Storing pending profile data for user: " + userKey);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pendingUsername_" + userKey, username);
        editor.putString("pendingPhone_" + userKey, phone);
        editor.putString("userEmail_" + userKey, email);
        if (encodedImage != null) {
            editor.putString("pendingProfilePic_" + userKey, encodedImage);
            Log.d(TAG, "Stored profile picture for user: " + userKey);
        }
        boolean saved = editor.commit();
        if (!saved) {
            Log.e(TAG, "Failed to save pending profile data");
            Toast.makeText(this, "Failed to save profile data", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Pending profile data stored successfully for user: " + userKey);
        showLoading(true);

        // Register user with Supabase
        supabaseAuth.signUp(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Signup success: " + message);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(SignupActivity.this,
                            "Registration successful! Please check your email to verify your account.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Signup error: " + error);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(SignupActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            signupButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
        }
    }
}
