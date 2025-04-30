package com.example.space;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private BottomNavigationView bottomNavigationView;

    // Key for saving active fragment state
    private static final String SELECTED_ITEM = "selected_item";
    private int selectedItem = R.id.nav_home;

    // Fragments
    private HomeFragment homeFragment;
    private ActivityFragment activityFragment;
    private ExploreFragment exploreFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme before setting content view
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        setContentView(R.layout.activity_main);

        // Initialize Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Initialize fragments
        homeFragment = new HomeFragment();
        activityFragment = new ActivityFragment();
        exploreFragment = new ExploreFragment();
        profileFragment = new ProfileFragment();

        // Check if we're coming back from a theme change
        String currentFragment = sharedPreferences.getString("current_fragment", null);

        if (currentFragment != null && currentFragment.equals("Settings")) {
            // Clear the stored fragment preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("current_fragment");
            editor.apply();

            // Load the settings fragment without adding to back stack
            SettingsFragment settingsFragment = new SettingsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fragment_container, settingsFragment)
                    .commit();

            return; // Skip the regular fragment loading
        }

        // Restore selected item from saved instance state
        if (savedInstanceState != null) {
            selectedItem = savedInstanceState.getInt(SELECTED_ITEM, R.id.nav_home);
        }

        // Set the saved/default selected item
        bottomNavigationView.setSelectedItemId(selectedItem);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ITEM, selectedItem);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        selectedItem = itemId;

        if (itemId == R.id.nav_home) {
            loadFragment(homeFragment);
            return true;
        } else if (itemId == R.id.nav_activity) {
            loadFragment(activityFragment);
            return true;
        } else if (itemId == R.id.nav_explore) {
            loadFragment(exploreFragment);
            return true;
        } else if (itemId == R.id.nav_profile) {
            loadFragment(profileFragment);
            return true;
        }

        return false;
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}