package com.example.space;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.space.adapters.TripImagePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TripDetailActivity extends AppCompatActivity {

    private String tripId;
    private String tripName;
    private String country;
    private String journal;
    private String startDateStr;
    private String endDateStr;
    private ArrayList<String> imageUrls;

    private TextView tripTitleTextView;
    private TextView tripDateTextView;
    private TextView countryTextView;
    private TextView journalTextView;
    private ViewPager2 imageViewPager;
    private TabLayout imageTabLayout;
    private ProgressBar progressBar;
    private ImageButton backButton;
    private SupabaseTrip supabaseTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        // Initialize Supabase client
        supabaseTrip = new SupabaseTrip(this);

        // Initialize views
        tripTitleTextView = findViewById(R.id.trip_title);
        tripDateTextView = findViewById(R.id.trip_date);
        countryTextView = findViewById(R.id.trip_country);
        journalTextView = findViewById(R.id.trip_journal);
        imageViewPager = findViewById(R.id.image_view_pager);
        imageTabLayout = findViewById(R.id.image_tab_layout);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.btn_back);

        // Setup toolbar with edit and delete actions
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Setup back button
        backButton.setOnClickListener(v -> finish());

        // Get trip data from intent
        getDataFromIntent();

        // Display trip data
        displayTripData();
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            tripId = intent.getStringExtra("trip_id");
            tripName = intent.getStringExtra("trip_name");
            country = intent.getStringExtra("country");
            journal = intent.getStringExtra("journal");
            startDateStr = intent.getStringExtra("start_date");
            endDateStr = intent.getStringExtra("end_date");
            imageUrls = intent.getStringArrayListExtra("image_urls");

            if (imageUrls == null) {
                imageUrls = new ArrayList<>();
            }
        }
    }

    private void displayTripData() {
        // Set trip title
        tripTitleTextView.setText(tripName);

        // Set country
        countryTextView.setText(country);

        // Set journal text
        journalTextView.setText(journal);

        // Format and set dates
        if (startDateStr != null && endDateStr != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                Date startDate = inputFormat.parse(startDateStr);
                Date endDate = inputFormat.parse(endDateStr);

                if (startDate != null && endDate != null) {
                    String formattedStartDate = outputFormat.format(startDate);
                    String formattedEndDate = outputFormat.format(endDate);
                    tripDateTextView.setText(String.format("%s - %s", formattedStartDate, formattedEndDate));
                }
            } catch (Exception e) {
                e.printStackTrace();
                tripDateTextView.setText("Date information unavailable");
            }
        } else {
            tripDateTextView.setText("Date information unavailable");
        }

        // Setup image slider if there are images
        if (!imageUrls.isEmpty()) {
            TripImagePagerAdapter adapter = new TripImagePagerAdapter(this, imageUrls);
            imageViewPager.setAdapter(adapter);

            // Connect TabLayout with ViewPager2
            new TabLayoutMediator(imageTabLayout, imageViewPager,
                    (tab, position) -> {
                        // No text needed for tabs
                    }).attach();

            // Show indicators only if there are multiple images
            if (imageUrls.size() > 1) {
                imageTabLayout.setVisibility(View.VISIBLE);
            } else {
                imageTabLayout.setVisibility(View.GONE);
            }
        } else {
            // Hide ViewPager and TabLayout if no images
            imageViewPager.setVisibility(View.GONE);
            imageTabLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trip_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            // Navigate to edit trip
            Intent editIntent = new Intent(this, AddTripActivity.class);
            editIntent.putExtra("trip_id", tripId);
            editIntent.putExtra("trip_name", tripName);
            editIntent.putExtra("country", country);
            editIntent.putExtra("journal", journal);
            editIntent.putExtra("start_date", startDateStr);
            editIntent.putExtra("end_date", endDateStr);
            editIntent.putStringArrayListExtra("image_urls", imageUrls);
            startActivity(editIntent);
            return true;
        } else if (id == R.id.action_delete) {
            // Show delete confirmation dialog
            showDeleteConfirmationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Trip");
        builder.setMessage("Are you sure you want to delete this trip? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteTrip());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteTrip() {
        progressBar.setVisibility(View.VISIBLE);

        supabaseTrip.deleteTrip(tripId, new SupabaseTrip.TripCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TripDetailActivity.this, "Trip deleted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity and return to the previous screen
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TripDetailActivity.this, "Error deleting trip: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}