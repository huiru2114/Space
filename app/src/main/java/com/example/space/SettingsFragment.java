package com.example.space;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private MaterialCardView appearanceCard;
    private MaterialCardView helpCard;
    private Switch darkModeSwitch;
    private TextView versionTextView;
    private ImageView backButton;

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Initialize views
        accountCard = view.findViewById(R.id.card_account);
        appearanceCard = view.findViewById(R.id.card_appearance);
        helpCard = view.findViewById(R.id.card_help);
        darkModeSwitch = view.findViewById(R.id.switch_dark_mode);
        versionTextView = view.findViewById(R.id.text_app_version);
        backButton = view.findViewById(R.id.btn_back);

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
        darkModeSwitch.setChecked(isDarkMode);

        // Set click listeners
        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        // Set back button click listener
        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        accountCard.setOnClickListener(v -> {
            AccountSettingsFragment accountFragment = new AccountSettingsFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, accountFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Dark mode switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.putString("current_fragment", "Settings");
            editor.apply();

            String message = isChecked ? "Dark mode enabled" : "Light mode enabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        helpCard.setOnClickListener(v -> {
            HelpCenterFragment helpFragment = new HelpCenterFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, helpFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
}