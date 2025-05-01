package com.example.space.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.space.ActivityFragment;
import com.example.space.R;

public class ExploreCountAdapter extends RecyclerView.Adapter<ExploreCountAdapter.ActivityViewHolder> {

    private ActivityFragment.ActivityItem[] activities;

    public ExploreCountAdapter(ActivityFragment.ActivityItem[] activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explore_countryact, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityFragment.ActivityItem activity = activities[position];
        holder.titleTextView.setText(activity.getTitle());
        holder.activityImageView.setImageResource(activity.getImageResId());
    }

    @Override
    public int getItemCount() {
        return activities.length;
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView activityImageView;
        TextView titleTextView;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            activityImageView = itemView.findViewById(R.id.activity_image);
            titleTextView = itemView.findViewById(R.id.activity_title);
        }
    }
}