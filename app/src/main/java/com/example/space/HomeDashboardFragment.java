package com.example.space;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

public class HomeDashboardFragment extends Fragment {

    private TextView tripCountTextView;
    private TextView countriesVisitedTextView;
    private RecyclerView upcomingTripsRecyclerView;
    private RecyclerView wishlistRecyclerView;
    private MaterialCardView statsCardView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        // Initialize views
        tripCountTextView = view.findViewById(R.id.text_trip_count);
        countriesVisitedTextView = view.findViewById(R.id.text_countries_visited);
        upcomingTripsRecyclerView = view.findViewById(R.id.recycler_upcoming_trips);
        wishlistRecyclerView = view.findViewById(R.id.recycler_wishlist);
        statsCardView = view.findViewById(R.id.card_travel_stats);

        // Setup RecyclerViews
        setupUpcomingTripsRecyclerView();
        setupWishlistRecyclerView();

        // Load user stats
        loadUserStats();

        return view;
    }

    private void setupUpcomingTripsRecyclerView() {
        upcomingTripsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // Set adapter - This would be your custom adapter for upcoming trips
        // upcomingTripsRecyclerView.setAdapter(new UpcomingTripsAdapter(getUpcomingTrips()));
    }

    private void setupWishlistRecyclerView() {
        wishlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // Set adapter - This would be your custom adapter for wishlist items
        // wishlistRecyclerView.setAdapter(new WishlistAdapter(getWishlistItems()));
    }

    private void loadUserStats() {
        // This would come from your data source or preferences
        int tripCount = 0; // Replace with actual count from your database
        int countriesVisited = 0; // Replace with actual count from your database

        // Update UI
        tripCountTextView.setText(String.valueOf(tripCount));
        countriesVisitedTextView.setText(String.valueOf(countriesVisited));

        // If user is not logged in or has no trips
        if (tripCount == 0) {
            statsCardView.setVisibility(View.GONE);
            // Show alternative content for new users
            View newUserView = getView().findViewById(R.id.layout_new_user);
            if (newUserView != null) {
                newUserView.setVisibility(View.VISIBLE);
            }
        }
    }
}