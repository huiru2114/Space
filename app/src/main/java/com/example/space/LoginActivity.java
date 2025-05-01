package com.example.space;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signupTextView;
    private ProgressBar progressBar;
    private ImageView backButton;
    private ImageView passwordVisibilityToggle;
    private boolean passwordVisible = false;

    private SupabaseAuth supabaseAuth;
    private SharedPreferences prefs;
    private SharedPreferences signupPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supabaseAuth = new SupabaseAuth(this);
        prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        signupPrefs = getSharedPreferences("SignupPrefs", MODE_PRIVATE);

        // Initialize views
        emailEditText = findViewById(R.id.et_username);
        passwordEditText = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.btn_login);
        signupTextView = findViewById(R.id.tv_signup);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);
        passwordVisibilityToggle = findViewById(R.id.password_visibility_toggle);

        // Pre-fill email if available from signup
        String savedEmail = prefs.getString("userEmail", "");
        if (!savedEmail.isEmpty()) {
            emailEditText.setText(savedEmail);
        }

        // Set listeners
        loginButton.setOnClickListener(v -> handleLogin());
        signupTextView.setOnClickListener(v -> navigateToSignup());
        backButton.setOnClickListener(v -> finish());
        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            // Show password
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordVisibilityToggle.setImageResource(R.drawable.ic_visibility_on);
        } else {
            // Hide password
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordVisibilityToggle.setImageResource(R.drawable.ic_visibility_off);
        }

        // Move cursor to the end of text
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

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

        showLoading(true);

        supabaseAuth.signIn(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Login success: " + message);

                // Store the email with user-specific key
                String userKey = email.replaceAll("[.@]", "_");
                prefs.edit().putString("userEmail_" + userKey, email).apply();

                // After successful login, check if profile exists
                String userId = supabaseAuth.getUserId();
                if (userId != null) {
                    checkUserProfile(userId);
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "Login successful but user ID is missing", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Login error: " + error);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void checkUserProfile(String userId) {
        Log.d(TAG, "Checking profile for user: " + userId);
        supabaseAuth.fetchProfile(userId, new SupabaseAuth.ProfileCallback() {
            @Override
            public void onSuccess(String username, String phone, String profilePic) {
                Log.d(TAG, "Profile exists - Username: " + username + ", Phone: " + phone + ", ProfilePic: " + profilePic);
                if (username == null || username.isEmpty()) {
                    // Profile exists but is empty, try to create it again
                    createProfileForUser(userId);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Welcome back, " + username + "!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onProfileNotFound() {
                Log.d(TAG, "Profile not found, creating new profile");
                createProfileForUser(userId);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking profile: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Error checking profile: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void createProfileForUser(String userId) {
        Log.d(TAG, "Creating profile for user: " + userId);
        String currentEmail = supabaseAuth.getCurrentUserEmail();
        if (currentEmail == null) {
            Log.e(TAG, "Current email is null");
            runOnUiThread(() -> {
                Toast.makeText(this, "Error: User email not found", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }

        // Get pending data from signup
        Object[] pendingData = supabaseAuth.getPendingProfileData(currentEmail);
        String username;
        String phone;

        if (pendingData != null) {
            username = (String) pendingData[0];
            phone = (String) pendingData[1];
            Log.d(TAG, "Using pending data - Username: " + username + ", Phone: " + phone);
        } else {
            // Generate username from email
            username = currentEmail.split("@")[0];
            phone = "";
            Log.d(TAG, "No pending data, using generated username: " + username);
        }

        supabaseAuth.createProfile(userId, username, phone, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Profile created successfully: " + message);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error creating profile: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Error creating profile: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    // We don't need onActivityResult anymore since we create the profile automatically

    private void navigateToSignup() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        }
    }
}
