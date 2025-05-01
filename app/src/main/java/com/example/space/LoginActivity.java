package com.example.space;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signupTextView;
    private ProgressBar progressBar;
    private ImageView backButton;
    private boolean passwordVisible = false;

    private SupabaseAuth supabaseAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supabaseAuth = new SupabaseAuth(this);
        prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);

        // Initialize views
        emailEditText = findViewById(R.id.et_username);
        passwordEditText = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.btn_login);
        signupTextView = findViewById(R.id.tv_signup);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);

        // Set up password visibility toggle with TextInputLayout
        TextInputLayout passwordLayout = findViewById(R.id.password_layout);

        // Set listeners
        loginButton.setOnClickListener(v -> handleLogin());
        signupTextView.setOnClickListener(v -> navigateToSignup());
        backButton.setOnClickListener(v -> finish());
    }

    // Navigate to login screen after email confirmation
    private void proceedToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Handle login logic
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

                String userKey = email.replaceAll("[.@]", "_");
                prefs.edit().putString("userEmail_" + userKey, email).apply();

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

    // Check if the user's profile exists and fetch details
    private void checkUserProfile(String userId) {
        supabaseAuth.fetchProfile(userId, new SupabaseAuth.ProfileCallback() {
            @Override
            public void onSuccess(String username, String phone, String profilePic) {
                if (username == null || username.isEmpty()) {
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
                createProfileForUser(userId);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Error checking profile: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    // Create profile for the user if none exists
    private void createProfileForUser(String userId) {
        String currentEmail = supabaseAuth.getCurrentUserEmail();
        if (currentEmail == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Error: User email not found", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }

        String username = currentEmail.split("@")[0];
        String phone = "";

        supabaseAuth.createProfile(userId, username, phone, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Error creating profile: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    // Navigate to Signup activity
    private void navigateToSignup() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    // Show or hide loading indicator
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