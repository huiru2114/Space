package com.example.space;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int LOGIN_REQUEST_CODE = 1001;

    private TextView usernameTextView;

    private View settingsButton;
    private View travelSummaryButton;

    private Button settingsTextButton;
    private Button travelSummaryTextButton;
    private ImageView profileImageView;
    private Button loginButton;
    private ProgressBar loadingIndicator;
    private SupabaseAuth supabaseAuth;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize SupabaseAuth
        supabaseAuth = new SupabaseAuth(requireContext());

        // Initialize views
        usernameTextView = view.findViewById(R.id.username_text);
        settingsButton = view.findViewById(R.id.btn_settings);
        settingsTextButton = view.findViewById(R.id.btn_settings_text);
        travelSummaryButton = view.findViewById(R.id.btn_travel_summary);
        travelSummaryTextButton = view.findViewById(R.id.btn_travel_summary_text);
        profileImageView = view.findViewById(R.id.profile_image);
        loginButton = view.findViewById(R.id.btn_login);
        loadingIndicator = view.findViewById(R.id.loading_indicator);

        prefs = getActivity().getSharedPreferences("AuthPrefs", MODE_PRIVATE);

        // Initialize UI with default state
        displayAnonymousState();

        // Check if user is authenticated
        validateSession();

        // Set click listeners
        settingsButton.setOnClickListener(v -> {
            navigateToSettings();
        });

        settingsTextButton.setOnClickListener(v -> {
            navigateToSettings();
        });

        // Set click listeners for travel summary
        travelSummaryButton.setOnClickListener(v -> {
            navigateToTravelSummary();
        });

        travelSummaryTextButton.setOnClickListener(v -> {
            navigateToTravelSummary();
        });

        // Set up login/logout button
        loginButton.setOnClickListener(v -> {
            if (supabaseAuth.getAccessToken() != null) {
                // User is logged in, perform logout
                supabaseAuth.signOut();
                // Note: No need to manually call displayAnonymousState() here
                // The AuthStateManager will notify all listeners and UI will update automatically
                Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            } else {
                // User is not logged in, navigate to login activity
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivityForResult(intent, LOGIN_REQUEST_CODE);
            }
        });
        return view;
    }

    /**
     * Navigate to the Settings fragment
     */
    private void navigateToSettings() {
        SettingsFragment settingsFragment = new SettingsFragment();
        replaceFragment(settingsFragment, "Settings");
    }

    /**
     * Navigate to the Travel Summary fragment
     */
    private void navigateToTravelSummary() {
        TravelSummaryFragment travelSummaryFragment = new TravelSummaryFragment();
        replaceFragment(travelSummaryFragment, "TravelSummary");
    }

    /**
     * Helper method to replace the current fragment
     */
    private void replaceFragment(Fragment fragment, String tag) {
        if (getActivity() != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment, tag);
            transaction.addToBackStack(tag);
            transaction.commit();
        } else {
            showToast("Error navigating. Please try again.");
        }
    }

    /**
     * Helper method to show toast messages
     */
    private void showToast(String message) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Checking login state");
        if (getActivity() != null) {
            validateSession();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == LOGIN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Login was successful, update the UI
            Log.d(TAG, "Login successful, updating UI");
            validateSession();
        }
    }

    /**
     * Validate if the user session is active and update UI accordingly
     */
    private void validateSession() {
        loadingIndicator.setVisibility(View.VISIBLE);

        // Check if we have an access token
        if (supabaseAuth.getAccessToken() != null) {
            supabaseAuth.validateSession(new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() == null) return;

                    Log.d(TAG, "Session valid, fetching profile");
                    getActivity().runOnUiThread(() -> {
                        // Session is valid, fetch user profile
                        fetchUserProfile();
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;

                    Log.e(TAG, "Session invalid: " + error);
                    getActivity().runOnUiThread(() -> {
                        // Session is invalid, display anonymous state
                        displayAnonymousState();
                    });
                }
            });
        } else {
            loadingIndicator.setVisibility(View.GONE);
            displayAnonymousState();
        }
    }

    /**
     * Display anonymous state UI
     */
    private void displayAnonymousState() {
        if (getActivity() == null) return;

        usernameTextView.setText("Anonymous");
        profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        loginButton.setText("Log In");

        // Clear any stored profile data
        prefs.edit()
                .remove("username")
                .remove("profilePic")
                .apply();
    }

    /**
     * Display authenticated state UI
     */
    private void displayAuthenticatedState(String username, String profilePicUrl) {
        if (getActivity() == null) return;

        usernameTextView.setText(username);
        loginButton.setText("Log Out");

        Log.d(TAG, "Displaying authenticated state - Username: " + username + ", ProfilePic: " + profilePicUrl);

        if (profilePicUrl != null && !profilePicUrl.isEmpty() && !profilePicUrl.equals("null")) {
            Log.d(TAG, "Loading profile picture from URL: " + profilePicUrl);
            loadProfileImage(profilePicUrl);
        } else {
            Log.d(TAG, "No profile picture URL available, using placeholder");
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            loadingIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * Fetch user profile from Supabase
     */
    private void fetchUserProfile() {
        if (getActivity() == null) return;

        String userId = supabaseAuth.getUserId();
        if (userId == null) {
            updateUIOnError("User ID not found");
            return;
        }

        loadingIndicator.setVisibility(View.VISIBLE);
        Log.d(TAG, "Fetching profile for user: " + userId);

        supabaseAuth.fetchProfile(userId, new SupabaseAuth.ProfileCallback() {
            @Override
            public void onSuccess(String username, String phone, String profilePic) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    displayAuthenticatedState(username, profilePic);
                });
            }

            @Override
            public void onProfileNotFound() {
                if (getActivity() == null) return;

                // Profile not found, create a new one
                createProfileForCurrentUser(userId);
            }

            @Override
            public void onError(String error) {
                updateUIOnError("Error: " + error);
            }
        });
    }

    /**
     * Create a new profile for the current user
     */
    private void createProfileForCurrentUser(String userId) {
        if (getActivity() == null) return;

        // Get current user's email
        String currentEmail = supabaseAuth.getCurrentUserEmail();
        if (currentEmail == null) {
            updateUIOnError("User email not found");
            return;
        }

        String userKey = currentEmail.replaceAll("[.@]", "_");

        // Check if we have a pending username from signup
        String username = prefs.getString("pendingUsername_" + userKey, null);
        String phone = prefs.getString("pendingPhone_" + userKey, "");

        // If no pending username, extract from email
        if (username == null || username.isEmpty()) {
            username = currentEmail.split("@")[0];
        }

        final String finalUsername = username;

        // Create profile
        supabaseAuth.createProfile(userId, finalUsername, phone, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    // Get potential profile pic URL from preferences
                    String profilePic = prefs.getString("profilePic_" + userKey, null);
                    displayAuthenticatedState(finalUsername, profilePic);
                    Toast.makeText(getActivity(), "Profile created", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                updateUIOnError("Failed to create profile: " + error);
            }
        });
    }

    /**
     * Load profile image with Glide
     */
    private void loadProfileImage(String imageUrl) {
        try {
            if (getActivity() != null) {
                Log.d(TAG, "Starting Glide image load for URL: " + imageUrl);

                // Create a custom GlideUrl with headers
                GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + supabaseAuth.getAccessToken())
                        .addHeader("apikey", getString(R.string.SUPABASE_KEY))
                        .addHeader("x-client-info", "supabase-android/1.0.0")
                        .build());

                Glide.with(this)
                        .load(glideUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .override(220, 220)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                if (e != null) {
                                    Log.e(TAG, "Failed to load profile image: " + e.getMessage());
                                    for (Throwable t : e.getRootCauses()) {
                                        Log.e(TAG, "Root cause: " + t.getMessage());
                                    }
                                }
                                loadingIndicator.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                Log.d(TAG, "Profile image loaded successfully");
                                loadingIndicator.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(profileImageView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage(), e);
            loadingIndicator.setVisibility(View.GONE);
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    /**
     * Update UI with error message
     */
    private void updateUIOnError(final String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                // Revert to anonymous state on error
                displayAnonymousState();
            });
        }
    }
}