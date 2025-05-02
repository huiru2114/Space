package com.example.space;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TravelSummaryFragment extends Fragment {

    // UI Components
    private TextView totalTripsValue;
    private TextView countriesVisitedValue;
    private TextView journalEntriesValue;
    private TextView favoriteDestinationValue;
    private TextView longestTripValue;
    private ImageButton backButton;
    private Button addTripButton;

    // Supabase helper
    private SupabaseTrip supabaseTrip;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the travel summary layout
        View view = inflater.inflate(R.layout.fragment_travel_summary, container, false);

        // Initialize UI components
        initViews(view);

        // Initialize Supabase helper
        supabaseTrip = new SupabaseTrip(requireContext());

        // Set up button listeners
        setupButtonListeners();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load travel data
        loadTravelData();
    }

    /**
     * Initialize all view components
     */
    private void initViews(View view) {
        // Initialize text views for travel stats
        totalTripsValue = view.findViewById(R.id.total_trips_value);
        countriesVisitedValue = view.findViewById(R.id.countries_visited_value);
        journalEntriesValue = view.findViewById(R.id.journal_entries_value);
        favoriteDestinationValue = view.findViewById(R.id.favorite_destination_value);
        longestTripValue = view.findViewById(R.id.longest_trip_value);

        // Initialize buttons
        backButton = view.findViewById(R.id.back_button);
        addTripButton = view.findViewById(R.id.add_trip_button);
    }

    /**
     * Set up button click listeners
     */
    private void setupButtonListeners() {
        // Set up back button
        backButton.setOnClickListener(v -> {
            // Navigate back to previous fragment/activity
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Set up add trip button
        addTripButton.setOnClickListener(v -> {
            // Launch AddTripActivity using an explicit Intent
            android.content.Intent intent = new android.content.Intent(getActivity(), AddTripActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Load travel data from Supabase
     */
    private void loadTravelData() {
        // Show loading state
        showLoadingState();

        // Fetch trips from Supabase
        supabaseTrip.getUserTrips(new SupabaseTrip.TripDataCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                // Calculate statistics from the trips
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        calculateAndDisplayStatistics(trips);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "Error loading trips: " + error, Toast.LENGTH_LONG).show();
                        // Display zero stats on error
                        displayTravelStatistics(0, 0, 0, "None", "None");
                    });
                }
            }
        });
    }

    /**
     * Show loading state while data is being fetched
     */
    private void showLoadingState() {
        totalTripsValue.setText("...");
        countriesVisitedValue.setText("...");
        journalEntriesValue.setText("...");
        favoriteDestinationValue.setText("...");
        longestTripValue.setText("...");
    }

    /**
     * Calculate statistics from the trips and display them
     */
    private void calculateAndDisplayStatistics(List<Trip> trips) {
        // Calculate total trips
        int totalTrips = trips.size();

        // Calculate unique countries visited
        Set<String> countries = new HashSet<>();
        for (Trip trip : trips) {
            if (trip.getCountry() != null && !trip.getCountry().isEmpty()) {
                countries.add(trip.getCountry());
            }
        }
        int countriesVisited = countries.size();

        // Count journal entries (assuming non-empty journal is one entry)
        int journalEntries = 0;
        for (Trip trip : trips) {
            if (trip.getJournal() != null && !trip.getJournal().trim().isEmpty()) {
                journalEntries++;
            }
        }

        // Find favorite destination (most visited country)
        String favoriteDestination = findFavoriteDestination(trips);

        // Find longest trip
        String longestTrip = findLongestTrip(trips);

        // Display the statistics
        displayTravelStatistics(totalTrips, countriesVisited, journalEntries,
                favoriteDestination, longestTrip);
    }

    /**
     * Find the most visited country (favorite destination)
     */
    private String findFavoriteDestination(List<Trip> trips) {
        if (trips.isEmpty()) {
            return "None";
        }

        // Count trips per country
        Map<String, Integer> countryCount = new HashMap<>();
        for (Trip trip : trips) {
            String country = trip.getCountry();
            if (country != null && !country.isEmpty()) {
                countryCount.put(country, countryCount.getOrDefault(country, 0) + 1);
            }
        }

        // Find country with most visits
        String favorite = "None";
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : countryCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favorite = entry.getKey();
            }
        }

        return favorite;
    }

    /**
     * Find the longest trip in days
     */
    private String findLongestTrip(List<Trip> trips) {
        if (trips.isEmpty()) {
            return "None";
        }

        long longestDuration = 0;
        Trip longestTrip = null;

        for (Trip trip : trips) {
            Date startDate = trip.getStartDate();
            Date endDate = trip.getEndDate();

            if (startDate != null && endDate != null) {
                long durationInMillis = endDate.getTime() - startDate.getTime();
                long durationInDays = TimeUnit.MILLISECONDS.toDays(durationInMillis);

                // Add 1 to include both start and end day
                durationInDays += 1;

                if (durationInDays > longestDuration) {
                    longestDuration = durationInDays;
                    longestTrip = trip;
                }
            }
        }

        if (longestTrip != null) {
            return longestDuration + " days (" + longestTrip.getTripName() + ")";
        } else {
            return "None";
        }
    }

    /**
     * Display travel statistics on the UI
     */
    private void displayTravelStatistics(int totalTrips, int countriesVisited,
                                         int journalEntries, String favoriteDestination,
                                         String longestTrip) {
        totalTripsValue.setText(String.valueOf(totalTrips));
        countriesVisitedValue.setText(String.valueOf(countriesVisited));
        journalEntriesValue.setText(String.valueOf(journalEntries));
        favoriteDestinationValue.setText(favoriteDestination);
        longestTripValue.setText(longestTrip);
    }

    /**
     * Optional: Method to update statistics when new data is available
     * Can be called from parent activity or other fragments
     */
    public void refreshStatistics() {
        if (isAdded()) {
            loadTravelData();
        }
    }
}