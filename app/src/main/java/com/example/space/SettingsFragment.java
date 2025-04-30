package com.example.space;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;

public class SettingsFragment extends Fragment {

    private MaterialCardView accountCard;
    private MaterialCardView notificationsCard;
    private MaterialCardView appearanceCard;
    private MaterialCardView privacyCard;
    private MaterialCardView helpCard;
    private Switch darkModeSwitch;
    private Switch notificationsSwitch;
    private TextView versionTextView;

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Initialize views
        accountCard = view.findViewById(R.id.card_account);
        notificationsCard = view.findViewById(R.id.card_notifications);
        appearanceCard = view.findViewById(R.id.card_appearance);
        privacyCard = view.findViewById(R.id.card_privacy);
        helpCard = view.findViewById(R.id.card_help);
        darkModeSwitch = view.findViewById(R.id.switch_dark_mode);
        notificationsSwitch = view.findViewById(R.id.switch_notifications);
        versionTextView = view.findViewById(R.id.text_app_version);

        // Set app version
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            versionTextView.setText("Version " + versionName);
        } catch (Exception e) {
            versionTextView.setText("Version Unknown");
        }

        // Set switch states based on saved preferences
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        boolean areNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        darkModeSwitch.setChecked(isDarkMode);
        notificationsSwitch.setChecked(areNotificationsEnabled);

        // Set click listeners
        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        accountCard.setOnClickListener(v -> {
            // Navigate to account management
            // For now, just show a toast since login functionality is handled elsewhere
            Toast.makeText(requireContext(), "Account management", Toast.LENGTH_SHORT).show();
        });

        // Dark mode switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();

            // Apply theme
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Notifications switch
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();

            String message = isChecked ? "Notifications enabled" : "Notifications disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        // Privacy settings
        privacyCard.setOnClickListener(v -> {
            // Navigate to privacy settings
            Toast.makeText(requireContext(), "Privacy settings", Toast.LENGTH_SHORT).show();
        });

        // Help center
        helpCard.setOnClickListener(v -> {
            // Navigate to help center
            Toast.makeText(requireContext(), "Help center", Toast.LENGTH_SHORT).show();
        });
    }
}