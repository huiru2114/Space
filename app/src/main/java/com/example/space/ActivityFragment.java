package com.example.space;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.space.adapters.ActivityAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ActivityFragment extends Fragment {

    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private FloatingActionButton addButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.activity_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Setup adapter with sample data
        adapter = new ActivityAdapter(createSampleActivities());
        recyclerView.setAdapter(adapter);

        // Setup FAB for adding new activities
        addButton = view.findViewById(R.id.fab_add_activity);
        addButton.setOnClickListener(v -> {
            // Navigate to AddTripActivity
            Intent intent = new Intent(getActivity(), AddTripActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private ActivityItem[] createSampleActivities() {
        return new ActivityItem[] {
                new ActivityItem("KL Unforgettable: Stories in the City", R.drawable.city_kl),
                new ActivityItem("KL Unforgettable: Stories in the City", R.drawable.city_kl),
                new ActivityItem("KL Unforgettable: Stories in the City", R.drawable.city_kl)
        };
    }

    // Model class for activity items
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
}