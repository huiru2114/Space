package com.example.space;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CountryTripsActivity extends AppCompatActivity {

    private TextView countryTitle;
    private ImageButton backButton;
    private RecyclerView tripsRecyclerView;
    private ProgressBar progressBar;
    private TripListAdapter tripAdapter;
    private SupabaseExplore supabaseExplore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_trips);

        // Initialize views
        countryTitle = findViewById(R.id.country_title);
        backButton = findViewById(R.id.btn_back);
        tripsRecyclerView = findViewById(R.id.trips_recycler_view);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize SupabaseExplore
        supabaseExplore = new SupabaseExplore(this);

        // Initialize adapter
        tripAdapter = new TripListAdapter(this);
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripsRecyclerView.setAdapter(tripAdapter);

        // Set back button listener
        backButton.setOnClickListener(v -> finish());

        // Get country from intent
        String country = getIntent().getStringExtra("selected_country");
        if (country != null && !country.isEmpty()) {
            countryTitle.setText(country);
            loadTripsByCountry(country);
        } else {
            Toast.makeText(this, "No country selected", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadTripsByCountry(String country) {
        progressBar.setVisibility(View.VISIBLE);

        supabaseExplore.getTripsByCountry(country, new SupabaseExplore.CountryTripsCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (trips.isEmpty()) {
                        Toast.makeText(CountryTripsActivity.this,
                                "No trips found for " + country, Toast.LENGTH_SHORT).show();
                    } else {
                        tripAdapter.setTrips(trips);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CountryTripsActivity.this,
                            "Error loading trips: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}