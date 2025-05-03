package com.example.space;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.space.adapters.TripAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityFragment extends Fragment implements TripAdapter.OnTripClickListener, AuthStateManager.AuthStateListener {

    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private FloatingActionButton addButton;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SupabaseTrip supabaseTrip;
    private List<Trip> tripList = new ArrayList<>();
    private TextView activityTitle;
    private TextView activitySubtitle;

    // Keep the original ActivityItem class to maintain compatibility
    public static class ActivityItem {
        private String title;
        private int imageResId;

        public ActivityItem(String title, int imageResId) {
            this.title = title;
            this.imageResId = imageResId;
        }

        public String getTitle() {
            return title;
        }

        public int getImageResId() {
            return imageResId;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        // Initialize title/subtitle views
        activityTitle = view.findViewById(R.id.activity_title);
        activitySubtitle = view.findViewById(R.id.activity_subtitle);

        // Initialize views
        recyclerView = view.findViewById(R.id.activity_recycler_view);

        // Try to find the new UI elements, but don't crash if they don't exist
        try {
            progressBar = view.findViewById(R.id.progress_bar);
            emptyStateTextView = view.findViewById(R.id.empty_state_text);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

            // Setup swipe to refresh
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(this::loadTrips);
            }
        } catch (Exception e) {
            // These views might not exist in the current layout
        }

        // Initialize RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Setup adapter with empty list initially
        adapter = new TripAdapter(getContext(), tripList, this);
        recyclerView.setAdapter(adapter);

        // Initialize Supabase client
        supabaseTrip = new SupabaseTrip(getContext());

        // Setup FAB for adding new activities
        addButton = view.findViewById(R.id.fab_add_activity);
        addButton.setOnClickListener(v -> {
            // Navigate to AddTripActivity
            Intent intent = new Intent(getActivity(), AddTripActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for auth state changes
        AuthStateManager.getInstance().addListener(this);

        // No need to call loadTrips() here as it will be called by the addListener callback
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister to prevent memory leaks
        AuthStateManager.getInstance().removeListener(this);
    }

    @Override
    public void onAuthStateChanged(boolean isAuthenticated) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (isAuthenticated) {
                // User is authenticated, show "MY TRIPS" UI
                activityTitle.setText("MY TRIPS");
                activitySubtitle.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.VISIBLE);
                loadTrips();
            } else {
                // User is not authenticated, show generic UI
                activityTitle.setText("TRIPS");
                activitySubtitle.setVisibility(View.GONE);
                addButton.setVisibility(View.GONE);
                tripList.clear();
                adapter.notifyDataSetChanged();

                if (emptyStateTextView != null) {
                    emptyStateTextView.setText("Please log in to view your trips");
                    emptyStateTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadTrips() {
        // Check authentication state first
        if (!AuthStateManager.getInstance().isAuthenticated()) {
            // Not authenticated, don't try to load trips
            if (emptyStateTextView != null) {
                emptyStateTextView.setText("Please log in to view your trips");
                emptyStateTextView.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Show progress bar if available
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Hide empty state text initially if available
        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.GONE);
        }

        // Load trips from Supabase
        supabaseTrip.getUserTrips(new SupabaseTrip.TripDataCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                if (getActivity() == null) return; // Fragment is no longer attached

                getActivity().runOnUiThread(() -> {
                    // Update the trip list
                    tripList.clear();
                    tripList.addAll(trips);
                    adapter.notifyDataSetChanged();

                    // Hide progress indicators if available
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    // Show empty state if no trips and view is available
                    if (emptyStateTextView != null) {
                        if (trips.isEmpty()) {
                            emptyStateTextView.setText("No trips found.\nAdd your first trip by clicking the + button.");
                            emptyStateTextView.setVisibility(View.VISIBLE);
                        } else {
                            emptyStateTextView.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return; // Fragment is no longer attached

                getActivity().runOnUiThread(() -> {
                    // Hide progress indicators if available
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    // Show error message
                    Toast.makeText(getContext(), "Error loading trips: " + error, Toast.LENGTH_SHORT).show();

                    // Show empty state if available
                    if (emptyStateTextView != null && tripList.isEmpty()) {
                        emptyStateTextView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    public void onTripClick(Trip trip) {
        // Navigate to TripDetailActivity
        Intent intent = new Intent(getActivity(), TripDetailActivity.class);
        intent.putExtra("trip_id", trip.getTripId());
        intent.putExtra("trip_name", trip.getTripName());
        intent.putExtra("country", trip.getCountry());
        intent.putExtra("journal", trip.getJournal());

        // Format dates to pass to the detail activity
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (trip.getStartDate() != null) {
            intent.putExtra("start_date", format.format(trip.getStartDate()));
        }
        if (trip.getEndDate() != null) {
            intent.putExtra("end_date", format.format(trip.getEndDate()));
        }

        // Pass image URLs as ArrayList
        ArrayList<String> imageUrls = new ArrayList<>(trip.getImageUrls());
        intent.putStringArrayListExtra("image_urls", imageUrls);

        startActivity(intent);
    }

    // For backward compatibility - create sample activities
    private ActivityItem[] createSampleActivities() {
        return new ActivityItem[] {
                new ActivityItem("KL Unforgettable: Stories in the City", R.drawable.ic_aeroplane),
                new ActivityItem("KL Unforgettable: Stories in the City", R.drawable.ic_aeroplane),
                new ActivityItem("KL Unforgettable: Stories in the City", R.drawable.ic_aeroplane)
        };
    }
}