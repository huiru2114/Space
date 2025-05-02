package com.example.space;

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

                        // Process dates if needed
                        for (Trip trip : trips) {
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