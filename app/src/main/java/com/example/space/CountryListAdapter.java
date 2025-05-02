package com.example.space;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CountryListAdapter extends RecyclerView.Adapter<CountryListAdapter.CountryViewHolder> {

    private List<String> countries;
    private Context context;
    private Map<String, String> countryCodeMap;

    public CountryListAdapter(Context context) {
        this.context = context;
        this.countries = new ArrayList<>();
        initializeCountryCodeMap();
    }

    /**
     * Initialize map of country names to ISO 2-letter codes
     */
    private void initializeCountryCodeMap() {
        countryCodeMap = new HashMap<>();
        // Add mappings for common countries
        String[] isoCodes = Locale.getISOCountries();
        for (String code : isoCodes) {
            Locale locale = new Locale("", code);
            String countryName = locale.getDisplayCountry(Locale.ENGLISH);
            countryCodeMap.put(countryName.toLowerCase(), code);
        }

        // Add some manual mappings for cases where the name might be different
        countryCodeMap.put("usa", "US");
        countryCodeMap.put("united states", "US");
        countryCodeMap.put("united states of america", "US");
        countryCodeMap.put("uk", "GB");
        countryCodeMap.put("united kingdom", "GB");
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

        // Add short description (you could customize this with real data)
        holder.countryDescription.setText(getRegionForCountry(country));

        // Load country flag
        loadCountryFlag(holder.countryFlag, country);

        // Set click listener for the country item
        holder.cardView.setOnClickListener(v -> {
            // Navigate to CountryTripsActivity with selected country
            Intent intent = new Intent(context, CountryTripsActivity.class);
            intent.putExtra("selected_country", country);
            context.startActivity(intent);
        });
    }

    /**
     * Returns a simple region description for a country
     * In a production app, you would get this from a real data source
     */
    private String getRegionForCountry(String country) {
        // This is a simple placeholder - in a real app, you'd use a proper data source
        Map<String, String> regionMap = new HashMap<>();
        regionMap.put("Malaysia", "Southeast Asia");
        regionMap.put("Japan", "East Asia");
        regionMap.put("France", "Western Europe");
        regionMap.put("Germany", "Central Europe");
        regionMap.put("United States", "North America");
        regionMap.put("Brazil", "South America");
        regionMap.put("Australia", "Oceania");
        regionMap.put("South Africa", "Southern Africa");

        return regionMap.getOrDefault(country, "");
    }

    /**
     * Load country flag using Glide and REST Countries API
     * @param imageView ImageView to load the flag into
     * @param countryName Country name to get flag for
     */
    private void loadCountryFlag(ImageView imageView, String countryName) {
        // Option 1: Using REST Countries API
        // String flagUrl = "https://restcountries.com/v3.1/name/" + countryName + "?fields=flags";

        // Option 2: Using FlagsAPI (more direct, just need country code)
        String countryCode = getCountryCode(countryName);
        if (countryCode != null) {
            String flagUrl = "https://flagsapi.com/" + countryCode + "/flat/64.png";

            Glide.with(context)
                    .load(flagUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_unknown)
                            .error(R.drawable.ic_unknown))
                    .into(imageView);
        } else {
            // If we couldn't find a country code, show placeholder
            imageView.setImageResource(R.drawable.ic_unknown);
        }
    }

    /**
     * Get ISO 2-letter country code from country name
     * @param countryName Country name
     * @return ISO 2-letter code or null if not found
     */
    private String getCountryCode(String countryName) {
        if (countryName == null || countryName.isEmpty()) {
            return null;
        }

        // Try lookup in our map
        return countryCodeMap.get(countryName.toLowerCase());
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
        TextView countryDescription;
        ImageView countryFlag;
        ImageView moreIcon;
        CardView cardView;

        public CountryViewHolder(@NonNull View itemView) {
            super(itemView);
            countryName = itemView.findViewById(R.id.country_name);
            countryDescription = itemView.findViewById(R.id.country_description);
            countryFlag = itemView.findViewById(R.id.country_flag);
            moreIcon = itemView.findViewById(R.id.more_icon);
            cardView = itemView.findViewById(R.id.country_card);
        }
    }
}