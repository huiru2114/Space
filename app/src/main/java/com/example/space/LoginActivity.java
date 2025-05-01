// LoginActivity.java
package com.example.space;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
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
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private ProgressBar progressBar;
    private ImageView passwordVisibilityToggle;
    private ImageView backButton;
    private boolean passwordVisible = false;
    private SupabaseAuth supabaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Supabase auth
        supabaseAuth = new SupabaseAuth(this);

        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvSignUp = findViewById(R.id.tv_signup);
        progressBar = findViewById(R.id.progress_bar);
        passwordVisibilityToggle = findViewById(R.id.password_visibility_toggle);
        backButton = findViewById(R.id.back_button);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up password visibility toggle
        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility());

        // Set up login button
        btnLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                performLogin();
            }
        });

        // Set up sign up text
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            // Show password
            etPassword.setTransformationMethod(null);
            passwordVisibilityToggle.setImageResource(R.drawable.ic_visibility_on);
        } else {
            // Hide password
            etPassword.setTransformationMethod(new PasswordTransformationMethod());
            passwordVisibilityToggle.setImageResource(R.drawable.ic_visibility_off);
        }
        // Move cursor to the end of the text
        etPassword.setSelection(etPassword.getText().length());
    }

    private boolean validateInputs() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate email
        if (TextUtils.isEmpty(email)) {
            etUsername.setError("Email is required");
            etUsername.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.setError("Please enter a valid email");
            etUsername.requestFocus();
            return false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void performLogin() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        supabaseAuth.signIn(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                    // Navigate back to the calling activity
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}