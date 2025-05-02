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

    // Flag to track if we came from Explore fragment
    private boolean fromExplore = false;
    // Error message passed from Explore fragment
    private String errorMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Add debug log to confirm activity creation
        Log.d(TAG, "LoginActivity onCreate called");

        supabaseAuth = new SupabaseAuth(this);
        prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);

        // Get intent extras
        Intent intent = getIntent();
        if (intent != null) {
            fromExplore = intent.getBooleanExtra("from_explore", false);
            errorMessage = intent.getStringExtra("error_message");

            // Add debug log
            Log.d(TAG, "fromExplore: " + fromExplore + ", errorMessage: " + errorMessage);
        }

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

        // Custom back button handler - fixed to ensure it always goes back to explore
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked, fromExplore: " + fromExplore);
            navigateBack();
        });

        // Show error message if provided
        if (errorMessage != null && !errorMessage.isEmpty()) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    public void onBackPressed() {
//        Log.d(TAG, "System back pressed, fromExplore: " + fromExplore);
//        navigateBack();
//    }

    // Method to handle both custom back button and system back button
    private void navigateBack() {
        Log.d(TAG, "navigateBack called, fromExplore: " + fromExplore);
        if (fromExplore) {
            // If we came from explore, send back a result with auth_required=true
            Intent resultIntent = new Intent();
            resultIntent.putExtra("auth_required", true);
            setResult(RESULT_CANCELED, resultIntent);

            // Log the result being set
            Log.d(TAG, "Setting result CANCELED with auth_required=true");

            finish();

            // Add a slide transition to make it visually clear we're going back
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
            // Regular finish for normal back navigation
            finish();
        }
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
                        handleSuccessfulLogin();
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
                        handleSuccessfulLogin();
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
                    handleSuccessfulLogin();
                });
            }
        });
    }

    // Handle successful login with proper navigation
    private void handleSuccessfulLogin() {
        showLoading(false);

        if (fromExplore) {
            // If we came from Explore, just set result and finish so it can reload
            Intent resultIntent = new Intent();
            resultIntent.putExtra("login_successful", true);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // Regular finish for normal login flow
            finish();
        }
    }

    // Create profile for the user if none exists
    private void createProfileForUser(String userId) {
        String currentEmail = supabaseAuth.getCurrentUserEmail();
        if (currentEmail == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Error: User email not found", Toast.LENGTH_SHORT).show();
                handleSuccessfulLogin();
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
                    handleSuccessfulLogin();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Error creating profile: " + error, Toast.LENGTH_SHORT).show();
                    handleSuccessfulLogin();
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
