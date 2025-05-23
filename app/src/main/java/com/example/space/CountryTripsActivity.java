package com.example.space;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CountryTripsActivity extends AppCompatActivity {

    private TextView countryTitle;
    private RecyclerView tripsRecyclerView;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private EditText searchTrip;
    private ImageButton filterButton;
    private TripListAdapter tripAdapter;
    private SupabaseExplore supabaseExplore;
    private String selectedCountry;
    private List<Trip> allTrips = new ArrayList<>();
    private static final int TRIP_DETAIL_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_trips);

        // Get selected country from intent
        selectedCountry = getIntent().getStringExtra("selected_country");
        if (selectedCountry == null) {
            Toast.makeText(this, "Error: No country selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        countryTitle = findViewById(R.id.country_title);
        tripsRecyclerView = findViewById(R.id.trips_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
        searchTrip = findViewById(R.id.search_trip);
        filterButton = findViewById(R.id.filter_button);

        // Set country title
        countryTitle.setText(selectedCountry);

        // Initialize adapter
        tripAdapter = new TripListAdapter(this);
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripsRecyclerView.setAdapter(tripAdapter);

        // Initialize Supabase service
        supabaseExplore = new SupabaseExplore(this);

        // Setup back button
        btnBack.setOnClickListener(v -> finish());

        // Setup search functionality
        setupSearch();

        // Setup filter button
        setupFilter();

        // Load trips for selected country
        loadTripsForCountry();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TRIP_DETAIL_REQUEST_CODE) {
            // Refresh trips regardless of result code to ensure we have the latest data
            // This covers both edits (RESULT_OK) and deletions (could be another result code)
            android.util.Log.d("CountryTripsActivity", "Returned from TripDetailActivity, refreshing trips");
            loadTripsForCountry();

            if (resultCode == RESULT_OK && data != null) {
                // Handle any specific data returned if needed
                boolean wasUpdated = data.getBooleanExtra("trip_updated", false);
                boolean wasDeleted = data.getBooleanExtra("trip_deleted", false);

                if (wasUpdated) {
                    Toast.makeText(this, "Trip updated successfully", Toast.LENGTH_SHORT).show();
                } else if (wasDeleted) {
                    Toast.makeText(this, "Trip deleted successfully", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Load trips for the selected country
     */
    private void loadTripsForCountry() {
        progressBar.setVisibility(View.VISIBLE);
        tripsRecyclerView.setVisibility(View.GONE);

        supabaseExplore.getTripsByCountry(selectedCountry, new SupabaseExplore.CountryTripsCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (trips.isEmpty()) {
                        Toast.makeText(CountryTripsActivity.this,
                                "No trips found for " + selectedCountry,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        tripsRecyclerView.setVisibility(View.VISIBLE);

                        // Process trips to ensure all needed fields are properly set
                        for (Trip trip : trips) {
                            // Make sure the trip_id is properly set
                            if (trip.getTripId() == null || trip.getTripId().isEmpty()) {
                                // Log a warning if trip_id is missing
                                android.util.Log.w("CountryTripsActivity",
                                        "Trip missing ID: " + trip.getTripName());
                            }

                            // Process other fields as needed
                            processTrip(trip);
                        }

                        allTrips = trips;
                        tripAdapter.setTrips(trips);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CountryTripsActivity.this,
                            "Error loading trips: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        }, new SupabaseExplore.AuthFailureCallback() {
            @Override
            public void onAuthFailure(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CountryTripsActivity.this,
                            message, Toast.LENGTH_SHORT).show();
                    // You might want to finish the activity or redirect to login
                    finish();
                });
            }
        });
    }

    /**
     * Process trip data to ensure dates are properly formatted
     * @param trip Trip to process
     */
    private void processTrip(Trip trip) {
        // If trip dates are strings, convert them to Date objects
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        try {
            // Only process if we have string dates that need conversion
            if (trip.getStartDate() == null && trip.getRawStartDate() != null) {
                Date startDate = apiFormat.parse(trip.getRawStartDate());
                trip.setStartDate(startDate);
            }

            if (trip.getEndDate() == null && trip.getRawEndDate() != null) {
                Date endDate = apiFormat.parse(trip.getRawEndDate());
                trip.setEndDate(endDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup search functionality
     */
    private void setupSearch() {
        searchTrip.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTrips(s.toString(), null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });
    }

    /**
     * Setup filter button
     */
    private void setupFilter() {
        filterButton.setOnClickListener(v -> {
            // Show filter dialog
            FilterDialogFragment dialogFragment = new FilterDialogFragment(
                    selectedFilter -> filterTrips(searchTrip.getText().toString(), selectedFilter)
            );
            dialogFragment.show(getSupportFragmentManager(), "FilterDialog");
        });
    }

    /**
     * Filter trips based on search text and selected filter
     * @param searchText Search text
     * @param filterOption Filter option (recent, oldest, etc.)
     */
    private void filterTrips(String searchText, String filterOption) {
        List<Trip> filteredList = new ArrayList<>();

        // First filter by search text
        if (searchText == null || searchText.isEmpty()) {
            filteredList.addAll(allTrips);
        } else {
            String lowercaseQuery = searchText.toLowerCase();
            for (Trip trip : allTrips) {
                if (trip.getTripName().toLowerCase().contains(lowercaseQuery)) {
                    filteredList.add(trip);
                }
            }
        }

        // Then apply date filter if selected
        if (filterOption != null) {
            switch (filterOption) {
                case "recent":
                    filteredList.sort((t1, t2) -> {
                        if (t1.getStartDate() == null || t2.getStartDate() == null) return 0;
                        return t2.getStartDate().compareTo(t1.getStartDate()); // Most recent first
                    });
                    break;
                case "oldest":
                    filteredList.sort((t1, t2) -> {
                        if (t1.getStartDate() == null || t2.getStartDate() == null) return 0;
                        return t1.getStartDate().compareTo(t2.getStartDate()); // Oldest first
                    });
                    break;
            }
        }

        tripAdapter.setTrips(filteredList);
    }
}