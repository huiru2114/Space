package com.example.space.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.space.R;
import com.example.space.Trip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private Context context;
    private List<Trip> trips;
    private OnTripClickListener listener;

    // Interface for click events
    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public TripAdapter(Context context, List<Trip> trips, OnTripClickListener listener) {
        this.context = context;
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_activity, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);

        // Set the trip title
        holder.titleTextView.setText(trip.getTripName());

        // Set the country name
        if (holder.locationDurationTextView != null) {
            holder.locationDurationTextView.setText(trip.getCountry());
        }

        // Load the first image as the card background
        if (trip.getImageUrls() != null && !trip.getImageUrls().isEmpty()) {
            String imageUrl = trip.getImageUrls().get(0);
            if (!TextUtils.isEmpty(imageUrl)) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .centerCrop()
                        .into(holder.activityImageView);
            } else {
                // Set a default image if no image URL is available
                holder.activityImageView.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            // Set a default image if no image URLs are available
            holder.activityImageView.setImageResource(R.drawable.placeholder_image);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTripClick(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        ImageView activityImageView;
        TextView titleTextView;
        TextView locationDurationTextView;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            activityImageView = itemView.findViewById(R.id.activity_image);
            titleTextView = itemView.findViewById(R.id.activity_title);
            locationDurationTextView = itemView.findViewById(R.id.location_duration);
        }
    }
}