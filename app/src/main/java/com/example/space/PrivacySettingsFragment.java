package com.example.space;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PrivacySettingsFragment extends Fragment {

    private ImageView backButton;
    private Switch locationSwitch;
    private Switch dataCollectionSwitch;
    private Switch personalizationSwitch;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy_settings, container, false);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Initialize views
        backButton = view.findViewById(R.id.btn_back);
        locationSwitch = view.findViewById(R.id.switch_location);
        dataCollectionSwitch = view.findViewById(R.id.switch_data_collection);
        personalizationSwitch = view.findViewById(R.id.switch_personalization);

        // Set back button click listener
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Load saved preferences
        boolean locationEnabled = sharedPreferences.getBoolean("privacy_location", false);
        boolean dataCollectionEnabled = sharedPreferences.getBoolean("privacy_data_collection", false);
        boolean personalizationEnabled = sharedPreferences.getBoolean("privacy_personalization", true);

        // Set switch states
        locationSwitch.setChecked(locationEnabled);
        dataCollectionSwitch.setChecked(dataCollectionEnabled);
        personalizationSwitch.setChecked(personalizationEnabled);

        // Set listeners
        setupSwitchListeners();

        return view;
    }

    private void setupSwitchListeners() {
        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("privacy_location", isChecked);
            editor.apply();

            String message = isChecked ? "Location sharing enabled" : "Location sharing disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        dataCollectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("privacy_data_collection", isChecked);
            editor.apply();

            String message = isChecked ? "Data collection enabled" : "Data collection disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        personalizationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("privacy_personalization", isChecked);
            editor.apply();

            String message = isChecked ? "Personalization enabled" : "Personalization disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
}