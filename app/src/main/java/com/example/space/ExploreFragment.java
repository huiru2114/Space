package com.example.space;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private static final String TAG = "ExploreFragment";
    private RecyclerView exploreRecyclerView;
    private CountryListAdapter countryAdapter;
    private SearchView searchDestination;
    private ProgressBar progressBar;
    private TextView noTripsText;
    private SupabaseExplore supabaseExplore;
    private SupabaseAuth supabaseAuth;
    private List<String> allCountries = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    // Login box components
    private CardView loginBoxCardView;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signupTextView;
    private ProgressBar loginProgressBar;
    private TextView loginMessageText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        // Initialize views
        exploreRecyclerView = view.findViewById(R.id.explore_results);
        searchDestination = view.findViewById(R.id.search_destination);
        progressBar = view.findViewById(R.id.explore_progress);
        noTripsText = view.findViewById(R.id.no_trips_text);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Initialize login box components
        loginBoxCardView = view.findViewById(R.id.login_box_card);
        emailEditText = view.findViewById(R.id.et_username);
        passwordEditText = view.findViewById(R.id.et_password);
        loginButton = view.findViewById(R.id.btn_login);
        signupTextView = view.findViewById(R.id.tv_signup);
        loginProgressBar = view.findViewById(R.id.login_progress_bar);
        loginMessageText = view.findViewById(R.id.login_message_text);

        // Initialize adapter
        countryAdapter = new CountryListAdapter(getContext());

        // Setup RecyclerView
        exploreRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        exploreRecyclerView.setAdapter(countryAdapter);

        // Initialize Supabase explore service
        supabaseExplore = new SupabaseExplore(getContext());
        supabaseAuth = new SupabaseAuth(getContext());

        // Setup search functionality
        setupSearch();

        // Setup swipe refresh layout
        setupSwipeRefresh();

        // Setup login box functionality
        setupLoginBox();

        return view;
    }

    /**
     * Setup login box functionality
     */
    private void setupLoginBox() {
        // Initially hide the login box
        loginBoxCardView.setVisibility(View.GONE);

        // Login button click listener
        loginButton.setOnClickListener(v -> {
            handleLogin();
        });

        // Signup text click listener
        signupTextView.setOnClickListener(v -> {
            // Navigate to signup activity
            Intent intent = new Intent(getContext(), SignupActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Handle login functionality
     */
    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
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

        // Show login progress
        showLoginLoading(true);

        // Attempt login
        supabaseAuth.signIn(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoginLoading(false);

                        // Hide login box
                        loginBoxCardView.setVisibility(View.GONE);

                        // Show success message
                        Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();

                        // Load user countries
                        loadUserCountries();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoginLoading(false);

                        // Show error message
                        loginMessageText.setText(error);
                        loginMessageText.setVisibility(View.VISIBLE);

                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Show or hide login loading indicators
     */
    private void showLoginLoading(boolean isLoading) {
        if (isLoading) {
            loginProgressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            loginMessageText.setVisibility(View.GONE);
        } else {
            loginProgressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
        }
    }

    /**
     * Setup swipe-to-refresh functionality
     */
    private void setupSwipeRefresh() {
        // Set refresh colors
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        // Set refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Clear search when refreshing
            searchDestination.setQuery("", false);
            searchDestination.clearFocus();

            // Force reload data from Supabase
            loadUserCountries();
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load countries when fragment is created
        loadUserCountries();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh countries list when returning to this fragment
        loadUserCountries();
    }

    /**
     * Show login required UI instead of navigating to LoginActivity
     */
    private void showLoginBox(String message) {
        // Make sure we're on the UI thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Show the login box
                loginBoxCardView.setVisibility(View.VISIBLE);

                // Set login message
                if (message != null) {
                    loginMessageText.setText(message);
                    loginMessageText.setVisibility(View.VISIBLE);
                } else {
                    loginMessageText.setVisibility(View.GONE);
                }

                // Hide progress and recycler view
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                if (exploreRecyclerView != null) {
                    exploreRecyclerView.setVisibility(View.GONE);
                }

                if (noTripsText != null) {
                    noTripsText.setVisibility(View.GONE);
                }

                // Cancel any refresh that might be in progress
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    /**
     * Load all countries that the user has visited from Supabase
     */
    private void loadUserCountries() {
        // Show progress indicator based on current state
        if (swipeRefreshLayout.isRefreshing()) {
            // If already showing swipe refresh indicator, don't show progress bar
            progressBar.setVisibility(View.GONE);
        } else {
            // Otherwise show the progress bar
            progressBar.setVisibility(View.VISIBLE);
            exploreRecyclerView.setVisibility(View.GONE);
            loginBoxCardView.setVisibility(View.GONE); // Hide login box
            if (noTripsText != null) noTripsText.setVisibility(View.GONE);
        }

        supabaseExplore.getUserCountries(new SupabaseExplore.ExploreCallback() {
            @Override
            public void onSuccess(List<String> countries) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // Hide all loading indicators
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        loginBoxCardView.setVisibility(View.GONE); // Hide login box

                        if (countries.isEmpty()) {
                            if (noTripsText != null) {
                                noTripsText.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(getContext(),
                                        "You haven't added any trips yet",
                                        Toast.LENGTH_SHORT).show();
                            }
                            exploreRecyclerView.setVisibility(View.GONE);
                        } else {
                            if (noTripsText != null) noTripsText.setVisibility(View.GONE);
                            exploreRecyclerView.setVisibility(View.VISIBLE);

                            // Standardize country names to avoid duplicates
                            List<String> standardizedCountries = standardizeCountryNames(countries);

                            allCountries = standardizedCountries;
                            countryAdapter.setCountries(standardizedCountries);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // Hide all loading indicators
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        // Check if error is authentication related
                        if (error.contains("Not authenticated") || error.contains("Please log in")) {
                            showLoginBox("Please log in to view your trips");
                        } else {
                            exploreRecyclerView.setVisibility(View.GONE);
                            Toast.makeText(getContext(),
                                    "Error loading countries: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }, new SupabaseExplore.AuthFailureCallback() {
            @Override
            public void onAuthFailure(String message) {
                // Handle auth failure by showing login box
                showLoginBox(message);
            }
        });
    }

    /**
     * Standardize country names to ensure consistent capitalization
     * @param countries List of countries from database
     * @return List of standardized country names
     */
    private List<String> standardizeCountryNames(List<String> countries) {
        List<String> standardized = new ArrayList<>();
        for (String country : countries) {
            if (country != null && !country.isEmpty()) {
                // Capitalize first letter of each word
                String[] words = country.split("\\s+");
                StringBuilder result = new StringBuilder();

                for (String word : words) {
                    if (!word.isEmpty()) {
                        result.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1).toLowerCase())
                                .append(" ");
                    }
                }
                standardized.add(result.toString().trim());
            } else {
                standardized.add(country); // Keep null/empty as is
            }
        }
        return standardized;
    }

    /**
     * Setup search functionality for countries
     */
    private void setupSearch() {
        searchDestination.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCountries(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCountries(newText);
                return true;
            }
        });

        // Clear button listener
        searchDestination.setOnCloseListener(() -> {
            countryAdapter.setCountries(allCountries);
            return false;
        });
    }

    /**
     * Filter countries based on search text
     * @param searchText Text to filter countries by
     */
    private void filterCountries(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            countryAdapter.setCountries(allCountries);
            return;
        }

        List<String> filteredList = new ArrayList<>();
        String lowercaseQuery = searchText.toLowerCase();

        for (String country : allCountries) {
            if (country.toLowerCase().contains(lowercaseQuery)) {
                filteredList.add(country);
            }
        }

        countryAdapter.setCountries(filteredList);

        if (filteredList.isEmpty() && noTripsText != null) {
            noTripsText.setText("No countries match your search");
            noTripsText.setVisibility(View.VISIBLE);
        } else if (noTripsText != null) {
            noTripsText.setVisibility(View.GONE);
        }
    }
}