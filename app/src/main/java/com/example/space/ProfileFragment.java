package com.example.space;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class ProfileFragment extends Fragment {

    private TextView usernameTextView;
    private View homeButton;
    private View settingsButton;
    private Button loginButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        usernameTextView = view.findViewById(R.id.username_text);
        homeButton = view.findViewById(R.id.btn_home);
        settingsButton = view.findViewById(R.id.btn_settings);
        loginButton = view.findViewById(R.id.btn_login);

        // Set default username as Anonymous
        usernameTextView.setText("Anonymous");

        // Set click listeners
        homeButton.setOnClickListener(v -> {
            //xy
            navigateToHomeDashboard();
        });

        settingsButton.setOnClickListener(v -> {
            //xy
            navigateToSettings();
        });

        loginButton.setOnClickListener(v -> {
            // Open login screen
        });

        return view;
    }

    //xy
    /**
     * Navigate to the Home Dashboard fragment
     */
    private void navigateToHomeDashboard() {
        HomeDashboardFragment homeDashboardFragment = new HomeDashboardFragment();
        replaceFragment(homeDashboardFragment, "HomeDashboard");
    }
//xy
    /**
     * Navigate to the Settings fragment
     */
    private void navigateToSettings() {
        SettingsFragment settingsFragment = new SettingsFragment();
        replaceFragment(settingsFragment, "Settings");
    }

//xy
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
//xy
    /**
     * Helper method to show toast messages
     */
    private void showToast(String message) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
    }


}