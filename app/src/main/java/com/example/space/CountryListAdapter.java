package com.example.space;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CountryListAdapter extends RecyclerView.Adapter<CountryListAdapter.CountryViewHolder> {

    private List<String> countries;
    private Context context;

    public CountryListAdapter(Context context) {
        this.context = context;
        this.countries = new ArrayList<>();
    }

    @NonNull
    @Override
    public CountryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_country, parent, false);
        return new CountryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CountryViewHolder holder, int position) {
        String country = countries.get(position);
        holder.countryName.setText(country);

        // Set click listener for the country item
        holder.cardView.setOnClickListener(v -> {
            // Navigate to CountryTripsActivity with selected country
            Intent intent = new Intent(context, CountryTripsActivity.class);
            intent.putExtra("selected_country", country);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return countries.size();
    }

    /**
     * Update the countries list and refresh the view
     * @param countries New list of countries
     */
    public void setCountries(List<String> countries) {
        this.countries = countries;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for country items
     */
    public static class CountryViewHolder extends RecyclerView.ViewHolder {
        TextView countryName;
        CardView cardView;

        public CountryViewHolder(@NonNull View itemView) {
            super(itemView);
            countryName = itemView.findViewById(R.id.country_name);
            cardView = itemView.findViewById(R.id.country_card);
        }
    }
}