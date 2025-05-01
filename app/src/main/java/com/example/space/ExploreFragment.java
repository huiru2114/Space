package com.example.space;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private RecyclerView exploreRecyclerView;
    private CountryListAdapter countryAdapter;
    private SearchView searchDestination;
    private ProgressBar progressBar;
    private TextView noTripsText;
    private SupabaseExplore supabaseExplore;
    private List<String> allCountries = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        // Initialize views
        exploreRecyclerView = view.findViewById(R.id.explore_results);
        searchDestination = view.findViewById(R.id.search_destination);
        progressBar = view.findViewById(R.id.explore_progress);
        noTripsText = view.findViewById(R.id.no_trips_text);

        // Initialize adapter
        countryAdapter = new CountryListAdapter(getContext());

        // Setup RecyclerView
        exploreRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        exploreRecyclerView.setAdapter(countryAdapter);

        // Initialize Supabase explore service
        supabaseExplore = new SupabaseExplore(getContext());

        // Setup search functionality
        setupSearch();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load countries when fragment is created
        loadUserCountries();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh countries list when returning to this fragment
        loadUserCountries();
    }

    /**
     * Load all countries that the user has visited from Supabase
     */
    private void loadUserCountries() {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        exploreRecyclerView.setVisibility(View.GONE);
        if (noTripsText != null) noTripsText.setVisibility(View.GONE);

        supabaseExplore.getUserCountries(new SupabaseExplore.ExploreCallback() {
            @Override
            public void onSuccess(List<String> countries) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        if (countries.isEmpty()) {
                            if (noTripsText != null) {
                                noTripsText.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(getContext(),
                                        "You haven't added any trips yet",
                                        Toast.LENGTH_SHORT).show();
                            }
                            exploreRecyclerView.setVisibility(View.GONE);
                        } else {
                            if (noTripsText != null) noTripsText.setVisibility(View.GONE);
                            exploreRecyclerView.setVisibility(View.VISIBLE);
                            allCountries = countries;
                            countryAdapter.setCountries(countries);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        exploreRecyclerView.setVisibility(View.GONE);

                        Toast.makeText(getContext(),
                                "Error loading countries: " + error,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Setup search functionality for countries
     */
    private void setupSearch() {
        searchDestination.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCountries(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCountries(newText);
                return true;
            }
        });

        // Clear button listener
        searchDestination.setOnCloseListener(() -> {
            countryAdapter.setCountries(allCountries);
            return false;
        });
    }

    /**
     * Filter countries based on search text
     * @param searchText Text to filter countries by
     */
    private void filterCountries(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            countryAdapter.setCountries(allCountries);
            return;
        }

        List<String> filteredList = new ArrayList<>();
        String lowercaseQuery = searchText.toLowerCase();

        for (String country : allCountries) {
            if (country.toLowerCase().contains(lowercaseQuery)) {
                filteredList.add(country);
            }
        }

        countryAdapter.setCountries(filteredList);

        if (filteredList.isEmpty() && noTripsText != null) {
            noTripsText.setText("No countries match your search");
            noTripsText.setVisibility(View.VISIBLE);
        } else if (noTripsText != null) {
            noTripsText.setVisibility(View.GONE);
        }
    }
}