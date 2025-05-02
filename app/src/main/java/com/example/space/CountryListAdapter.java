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
        Map<String, String> regionMap = new HashMap<>();

        // Southeast Asia
        regionMap.put("Malaysia", "Southeast Asia");
        regionMap.put("Indonesia", "Southeast Asia");
        regionMap.put("Philippines", "Southeast Asia");
        regionMap.put("Thailand", "Southeast Asia");
        regionMap.put("Vietnam", "Southeast Asia");
        regionMap.put("Singapore", "Southeast Asia");
        regionMap.put("Myanmar", "Southeast Asia");

        // East Asia
        regionMap.put("Japan", "East Asia");
        regionMap.put("South Korea", "East Asia");
        regionMap.put("China", "East Asia");
        regionMap.put("Taiwan", "East Asia");
        regionMap.put("Mongolia", "East Asia");

        // South Asia
        regionMap.put("India", "South Asia");
        regionMap.put("Pakistan", "South Asia");
        regionMap.put("Bangladesh", "South Asia");
        regionMap.put("Nepal", "South Asia");
        regionMap.put("Sri Lanka", "South Asia");

        // Middle East
        regionMap.put("Saudi Arabia", "Middle East");
        regionMap.put("United Arab Emirates", "Middle East");
        regionMap.put("Iran", "Middle East");
        regionMap.put("Iraq", "Middle East");
        regionMap.put("Israel", "Middle East");
        regionMap.put("Jordan", "Middle East");
        regionMap.put("Turkey", "Middle East");

        // Western Europe
        regionMap.put("France", "Western Europe");
        regionMap.put("Germany", "Western Europe");
        regionMap.put("Belgium", "Western Europe");
        regionMap.put("Netherlands", "Western Europe");
        regionMap.put("Austria", "Western Europe");

        // Southern Europe
        regionMap.put("Italy", "Southern Europe");
        regionMap.put("Spain", "Southern Europe");
        regionMap.put("Portugal", "Southern Europe");
        regionMap.put("Greece", "Southern Europe");

        // Northern Europe
        regionMap.put("United Kingdom", "Northern Europe");
        regionMap.put("Ireland", "Northern Europe");
        regionMap.put("Norway", "Northern Europe");
        regionMap.put("Sweden", "Northern Europe");
        regionMap.put("Denmark", "Northern Europe");
        regionMap.put("Finland", "Northern Europe");

        // Eastern Europe
        regionMap.put("Poland", "Eastern Europe");
        regionMap.put("Ukraine", "Eastern Europe");
        regionMap.put("Russia", "Eastern Europe");
        regionMap.put("Czech Republic", "Eastern Europe");
        regionMap.put("Hungary", "Eastern Europe");

        // North America
        regionMap.put("United States", "North America");
        regionMap.put("Canada", "North America");
        regionMap.put("Mexico", "North America");

        // Central America & Caribbean
        regionMap.put("Cuba", "Caribbean");
        regionMap.put("Jamaica", "Caribbean");
        regionMap.put("Costa Rica", "Central America");
        regionMap.put("Panama", "Central America");

        // South America
        regionMap.put("Brazil", "South America");
        regionMap.put("Argentina", "South America");
        regionMap.put("Chile", "South America");
        regionMap.put("Colombia", "South America");
        regionMap.put("Peru", "South America");

        // Oceania
        regionMap.put("Australia", "Oceania");
        regionMap.put("New Zealand", "Oceania");
        regionMap.put("Fiji", "Oceania");
        regionMap.put("Papua New Guinea", "Oceania");

        // North Africa
        regionMap.put("Egypt", "North Africa");
        regionMap.put("Algeria", "North Africa");
        regionMap.put("Morocco", "North Africa");
        regionMap.put("Tunisia", "North Africa");

        // West Africa
        regionMap.put("Nigeria", "West Africa");
        regionMap.put("Ghana", "West Africa");
        regionMap.put("Ivory Coast", "West Africa");
        regionMap.put("Senegal", "West Africa");

        // East Africa
        regionMap.put("Kenya", "East Africa");
        regionMap.put("Ethiopia", "East Africa");
        regionMap.put("Tanzania", "East Africa");
        regionMap.put("Uganda", "East Africa");

        // Southern Africa
        regionMap.put("South Africa", "Southern Africa");
        regionMap.put("Namibia", "Southern Africa");
        regionMap.put("Botswana", "Southern Africa");
        regionMap.put("Zimbabwe", "Southern Africa");

        // If not found, return "Welcome to <Country>"
        return regionMap.containsKey(country)
                ? regionMap.get(country)
                : "Welcome to " + country;
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