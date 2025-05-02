package com.example.space;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private RecyclerView exploreRecyclerView;
    private CountryListAdapter countryAdapter;
    private SearchView searchDestination;
    private ProgressBar progressBar;
    private TextView noTripsText;
    private SupabaseExplore supabaseExplore;
    private List<String> allCountries = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout; // Added SwipeRefreshLayout
    private static final int REQUEST_LOGIN = 1001;

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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout); // Initialize SwipeRefreshLayout

        // Initialize adapter
        countryAdapter = new CountryListAdapter(getContext());

        // Setup RecyclerView
        exploreRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        exploreRecyclerView.setAdapter(countryAdapter);

        // Initialize Supabase explore service
        supabaseExplore = new SupabaseExplore(getContext());

        // Setup search functionality
        setupSearch();

        // Setup swipe refresh layout
        setupSwipeRefresh();

        return view;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                // User successfully logged in, reload data
                loadUserCountries();
            } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
                // User pressed back button from login
                boolean authRequired = data.getBooleanExtra("auth_required", false);
                if (authRequired) {
                    // Show a message that login is required
                    Toast.makeText(getContext(),
                            "Please log in to view your trips",
                            Toast.LENGTH_SHORT).show();

                    // Show a more prominent message
                    if (noTripsText != null) {
                        noTripsText.setText("Please log in to view your trips");
                        noTripsText.setVisibility(View.VISIBLE);
                    }

                    // Hide progress and recycler view
                    progressBar.setVisibility(View.GONE);
                    exploreRecyclerView.setVisibility(View.GONE);
                }
            }
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
                        exploreRecyclerView.setVisibility(View.GONE);

                        Toast.makeText(getContext(),
                                "Error loading countries: " + error,
                                Toast.LENGTH_SHORT).show();
                    });
                }
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