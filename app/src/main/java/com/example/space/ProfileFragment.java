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

public class ProfileFragment extends Fragment {

    private TextView usernameTextView;
    private View homeButton;
    private View settingsButton;
    private Button loginButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        // Initialize views
        usernameTextView = view.findViewById(R.id.username_text);
        homeButton = view.findViewById(R.id.btn_home);
        settingsButton = view.findViewById(R.id.btn_settings);
        loginButton = view.findViewById(R.id.btn_login);

        // Set default username as Anonymous
        usernameTextView.setText("Anonymous");

        // Set click listeners
        homeButton.setOnClickListener(v -> {
            // Navigate to home
        });

        settingsButton.setOnClickListener(v -> {
            // Open settings
        });

        loginButton.setOnClickListener(v -> {
            // Open login screen
        });

        return view;
    }
}