package com.example.space;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class SupabaseExplore {
    private static final String TAG = "SupabaseExplore";
    private static final String TRIPS_TABLE = "trips";
    private String SUPABASE_URL;
    private String API_KEY;
    private String REST_URL;
    private SupabaseAuth auth;
    private ExecutorService executor;
    private Context context;

    // Constructor
    public SupabaseExplore(Context context) {
        this.context = context;
        this.SUPABASE_URL = context.getResources().getString(R.string.SUPABASE_URL);
        this.API_KEY = context.getResources().getString(R.string.SUPABASE_KEY);
        this.REST_URL = SUPABASE_URL + "/rest/v1";
        this.auth = new SupabaseAuth(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Interface for explore operations callbacks
     */
    public interface ExploreCallback {
        void onSuccess(List<String> countries);
        void onError(String error);
    }

    /**
     * Interface for trips by country operations callbacks
     */
    public interface CountryTripsCallback {
        void onSuccess(List<Trip> trips);
        void onError(String error);
    }

    /**
     * Get all countries the user has visited, with token refresh handling
     * @param callback Callback to handle the response
     */
    public void getUserCountries(ExploreCallback callback) {
        getUserCountriesInternal(callback, false);
    }

    /**
     * Internal method to get user countries with token refresh capability
     * @param callback Callback to handle the response
     * @param isRetry Whether this is a retry after token refresh
     */
    private void getUserCountriesInternal(ExploreCallback callback, boolean isRetry) {
        executor.execute(() -> {
            try {
                String accessToken = auth.getAccessToken();
                if (accessToken == null) {
                    Log.e(TAG, "No access token found");
                    handleAuthFailure("Not authenticated. Please log in.");
                    callback.onError("Not authenticated. Please log in.");
                    return;
                }

                // Make sure we have the current user ID
                String userId = auth.getUserId();
                if (userId == null) {
                    Log.e(TAG, "User ID not found");
                    handleAuthFailure("User ID not found. Please log in.");
                    callback.onError("User ID not found. Please log in.");
                    return;
                }

                // Build the URL with query parameters to get trips for the current user
                URL url = new URL(REST_URL + "/" + TRIPS_TABLE + "?user_id=eq." + userId + "&select=country");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "User countries fetch response code: " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    // Success - Parse the response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseStr = response.toString();
                    Log.d(TAG, "Countries response: " + responseStr);

                    // Parse JSON array response
                    JSONArray jsonArray = new JSONArray(responseStr);
                    Set<String> uniqueCountries = new HashSet<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject tripObj = jsonArray.getJSONObject(i);
                        String country = tripObj.getString("country");
                        uniqueCountries.add(country);
                    }

                    List<String> countries = new ArrayList<>(uniqueCountries);
                    callback.onSuccess(countries);
                } else {
                    // Error getting countries
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Countries fetch error response: " + errorResponse);

                    // Check if the token has expired
                    if (errorResponse.contains("JWT expired") && !isRetry) {
                        // Try to refresh the token and retry the request
                        refreshTokenAndRetry(() -> getUserCountriesInternal(callback, true),
                                error -> callback.onError(error));
                    } else {
                        callback.onError("Failed to fetch countries: " + errorResponse);
                    }
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error fetching countries: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Get trips for a specific country
     * @param country Country to get trips for
     * @param callback Callback to handle the response
     */
    public void getTripsByCountry(String country, CountryTripsCallback callback) {
        getTripsByCountryInternal(country, callback, false);
    }

    /**
     * Internal method to get trips by country with token refresh capability
     * @param country Country to get trips for
     * @param callback Callback to handle the response
     * @param isRetry Whether this is a retry after token refresh
     */
    private void getTripsByCountryInternal(String country, CountryTripsCallback callback, boolean isRetry) {
        executor.execute(() -> {
            try {
                String accessToken = auth.getAccessToken();
                if (accessToken == null) {
                    Log.e(TAG, "No access token found");
                    handleAuthFailure("Not authenticated. Please log in.");
                    callback.onError("Not authenticated. Please log in.");
                    return;
                }

                // Make sure we have the current user ID
                String userId = auth.getUserId();
                if (userId == null) {
                    Log.e(TAG, "User ID not found");
                    handleAuthFailure("User ID not found. Please log in.");
                    callback.onError("User ID not found. Please log in.");
                    return;
                }

                // Build the URL with query parameters to get trips for the current user and country
                URL url = new URL(REST_URL + "/" + TRIPS_TABLE +
                        "?user_id=eq." + userId +
                        "&country=eq." + country);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Trips by country fetch response code: " + responseCode);

                if (responseCode >= 200 && responseCode < 300) {
                    // Success - Parse the response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseStr = response.toString();
                    Log.d(TAG, "Trips by country response: " + responseStr);

                    // Parse JSON array response
                    JSONArray jsonArray = new JSONArray(responseStr);
                    List<Trip> trips = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject tripObj = jsonArray.getJSONObject(i);

                        Trip trip = new Trip();
                        trip.setId(tripObj.getString("id"));
                        trip.setUserId(tripObj.getString("user_id"));
                        trip.setTripName(tripObj.getString("trip_name"));
                        trip.setCountry(tripObj.getString("country"));

                        if (!tripObj.isNull("journal")) {
                            trip.setJournal(tripObj.getString("journal"));
                        }

                        // Parse dates - would need to convert string dates to Date objects

                        // Parse image URLs if they exist
                        if (!tripObj.isNull("image_url")) {
                            JSONArray imageUrls = tripObj.getJSONArray("image_url");
                            for (int j = 0; j < imageUrls.length(); j++) {
                                trip.addImageUrl(imageUrls.getString(j));
                            }
                        }

                        trips.add(trip);
                    }

                    callback.onSuccess(trips);
                } else {
                    // Error getting trips
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Trips fetch error response: " + errorResponse);

                    // Check if the token has expired
                    if (errorResponse.contains("JWT expired") && !isRetry) {
                        // Try to refresh the token and retry the request
                        refreshTokenAndRetry(() -> getTripsByCountryInternal(country, callback, true),
                                error -> callback.onError(error));
                    } else {
                        callback.onError("Failed to fetch trips: " + errorResponse);
                    }
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error fetching trips: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Interface for retry operations
     */
    private interface RetryOperation {
        void retry();
    }

    /**
     * Interface for error callback
     */
    private interface ErrorCallback {
        void onError(String error);
    }

    /**
     * Refresh token and retry operation
     * @param retryOperation Operation to retry after token refresh
     * @param errorCallback Callback to handle errors
     */
    private void refreshTokenAndRetry(RetryOperation retryOperation, ErrorCallback errorCallback) {
        executor.execute(() -> {
            try {
                // Use the auth class to refresh token
                boolean refreshed = auth.refreshToken();

                if (refreshed) {
                    // Retry the operation
                    retryOperation.retry();
                } else {
                    // Handle refresh failure
                    Log.e(TAG, "Token refresh failed");
                    handleAuthFailure("Session expired. Please log in again.");
                    errorCallback.onError("Session expired. Please log in again.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing token: " + e.getMessage(), e);
                handleAuthFailure("Authentication error: " + e.getMessage());
                errorCallback.onError("Authentication error: " + e.getMessage());
            }
        });
    }

    /**
     * Handle authentication failure by redirecting to login
     * @param message Error message to display
     */
    private void handleAuthFailure(String message) {
        if (context != null) {
            // Run on UI thread
            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
            mainHandler.post(() -> {
                // Clear stored credentials
                auth.signOut();

                // Redirect to login screen
                Intent intent = new Intent(context, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("error_message", message);
                context.startActivity(intent);
            });
        }
    }
}