package com.example.space;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripListAdapter extends RecyclerView.Adapter<TripListAdapter.TripViewHolder> {

    private List<Trip> trips;
    private Context context;
    private SimpleDateFormat dateFormat;

    public TripListAdapter(Context context) {
        this.context = context;
        this.trips = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.explore_item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);

        // Set trip data
        holder.tripName.setText(trip.getTripName());

        // Set date range if available
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            // Check if start and end dates are the same
            if (trip.getStartDate().equals(trip.getEndDate())) {
                // Only show single date
                holder.tripDates.setText(dateFormat.format(trip.getStartDate()));
            } else {
                // Show date range
                String dateRange = dateFormat.format(trip.getStartDate()) +
                        " - " +
                        dateFormat.format(trip.getEndDate());
                holder.tripDates.setText(dateRange);
            }
            holder.tripDates.setVisibility(View.VISIBLE);
        } else {
            holder.tripDates.setVisibility(View.GONE);
        }

        // Load trip image if available
        if (trip.getImageUrls() != null && !trip.getImageUrls().isEmpty()) {
            String firstImageUrl = trip.getImageUrls().get(0);
            Glide.with(context)
                    .load(firstImageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_pic_unknown)
                    .into(holder.tripImage);
        } else {
            // Set default image or hide image view
            holder.tripImage.setImageResource(R.drawable.ic_pic_unknown);
        }

        // Set click listener - can be used to navigate to trip details
        holder.cardView.setOnClickListener(v -> {
            // Create intent to navigate to trip details
            Intent intent = new Intent(context, TripDetailActivity.class);

            // Pass trip details to the detail activity
            intent.putExtra("trip_id", trip.getTripId()); // Assuming Trip class has getId method
            intent.putExtra("trip_name", trip.getTripName());
            intent.putExtra("country", trip.getCountry());
            intent.putExtra("journal", trip.getJournal());

            // Format dates to strings if they exist
            if (trip.getStartDate() != null) {
                SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                intent.putExtra("start_date", apiFormat.format(trip.getStartDate()));
            } else if (trip.getRawStartDate() != null) {
                intent.putExtra("start_date", trip.getRawStartDate());
            }

            if (trip.getEndDate() != null) {
                SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                intent.putExtra("end_date", apiFormat.format(trip.getEndDate()));
            } else if (trip.getRawEndDate() != null) {
                intent.putExtra("end_date", trip.getRawEndDate());
            }

            // Add image URLs if available
            if (trip.getImageUrls() != null && !trip.getImageUrls().isEmpty()) {
                ArrayList<String> imageUrlList = new ArrayList<>(trip.getImageUrls());
                intent.putStringArrayListExtra("image_urls", imageUrlList);
            }

            // Start the trip detail activity
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    public void setTrips(List<Trip> trips) {
        this.trips = trips;
        notifyDataSetChanged();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tripName;
        TextView tripDates;
        ImageView tripImage;
        CardView cardView;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tripName = itemView.findViewById(R.id.trip_name);
            tripDates = itemView.findViewById(R.id.trip_dates);
            tripImage = itemView.findViewById(R.id.trip_image);
            cardView = itemView.findViewById(R.id.trip_card);
        }
    }
}