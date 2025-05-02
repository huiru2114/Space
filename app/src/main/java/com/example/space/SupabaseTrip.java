package com.example.space;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class SupabaseTrip {
    private static final String TAG = "SupabaseTrip";
    private static final String TRIPS_TABLE = "trips";
    private static final String TRIP_IMAGES_BUCKET = "trip-images";
    private String SUPABASE_URL;
    private String API_KEY;
    private String REST_URL;
    private String STORAGE_URL;
    private SupabaseAuth auth;
    private ExecutorService executor;

    // Constructor
    public SupabaseTrip(Context context) {
        this.SUPABASE_URL = context.getResources().getString(R.string.SUPABASE_URL);
        this.API_KEY = context.getResources().getString(R.string.SUPABASE_KEY);
        this.REST_URL = SUPABASE_URL + "/rest/v1";
        this.STORAGE_URL = SUPABASE_URL + "/storage/v1";
        this.auth = new SupabaseAuth(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Interface for trip operations callbacks
     */
    public interface TripCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Interface for retrieving trip data
     */
    public interface TripDataCallback {
        void onSuccess(List<Trip> trips);
        void onError(String error);
    }

    /**
     * Save a trip to Supabase
     * @param trip Trip object to save
     * @param callback Callback to handle the response
     */
    public void saveTrip(Trip trip, TripCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting trip creation process");
                String accessToken = auth.getAccessToken();
                if (accessToken == null) {
                    Log.e(TAG, "No access token found");
                    callback.onError("Not authenticated. Please log in.");
                    return;
                }

                // Make sure we have the current user ID
                String userId = auth.getUserId();
                if (userId == null) {
                    Log.e(TAG, "User ID not found");
                    callback.onError("User ID not found. Please log in.");
                    return;
                }

                // Set the user ID for the trip
                trip.setUserId(userId);

                // Create JSON payload for creating trip
                JSONObject payload = new JSONObject();

                // Only set trip_id for updates, not for new trips (let the database generate it)
                if (trip.getTripId() != null && !trip.getTripId().isEmpty()) {
                    payload.put("trip_id", trip.getTripId());
                }

                payload.put("user_id", trip.getUserId());
                payload.put("trip_name", trip.getTripName());
                payload.put("country", trip.getCountry());
                payload.put("journal", trip.getJournal());

                // Format dates for SQL (YYYY-MM-DD)
                SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                payload.put("start_date", sqlDateFormat.format(trip.getStartDate()));
                payload.put("end_date", sqlDateFormat.format(trip.getEndDate()));

                // Handle image URLs as JSON array
                JSONArray imageUrlsArray = trip.getImageUrlsAsJsonArray();
                payload.put("image_url", imageUrlsArray);

                Log.d(TAG, "Sending trip creation request with payload: " + payload.toString());

                // Determine if this is an update or create
                boolean isUpdate = trip.getTripId() != null && !trip.getTripId().isEmpty();
                String endpoint = REST_URL + "/" + TRIPS_TABLE;
                String method = isUpdate ? "PATCH" : "POST";

                // If updating, add query parameter to specify which trip to update
                if (isUpdate) {
                    endpoint += "?trip_id=eq." + trip.getTripId();
                }

                // Make the API call to create/update trip
                URL url = new URL(endpoint);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Prefer", "return=representation");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Trip operation response code: " + responseCode);

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
                    Log.d(TAG, "Trip operation response: " + responseStr);

                    // Get the trip_id from the response for new trips
                    if (!isUpdate && responseStr != null && !responseStr.isEmpty() && !responseStr.equals("[]")) {
                        try {
                            JSONArray responseArray = new JSONArray(responseStr);
                            if (responseArray.length() > 0) {
                                JSONObject tripObj = responseArray.getJSONObject(0);
                                String tripId = tripObj.optString("trip_id", "");
                                if (!tripId.isEmpty()) {
                                    trip.setTripId(tripId);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing trip_id from response: " + e.getMessage());
                        }
                    }

                    String successMessage = isUpdate ? "Trip updated successfully" : "Trip created successfully";
                    callback.onSuccess(successMessage);
                } else {
                    // Error creating/updating trip
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Trip operation error response: " + errorResponse);

                    // Special handling for common errors
                    if (errorResponse.contains("violates foreign key constraint")) {
                        callback.onError("Cannot create trip: Your user profile hasn't been created. Please set up your profile first.");
                    } else {
                        callback.onError("Failed to " + (isUpdate ? "update" : "create") + " trip: " + errorResponse);
                    }
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error creating/updating trip: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Get trips for the current user
     * @param callback Callback to handle the response
     */
    public void getUserTrips(TripDataCallback callback) {
        executor.execute(() -> {
            try {
                String accessToken = auth.getAccessToken();
                if (accessToken == null) {
                    callback.onError("Not authenticated. Please log in.");
                    return;
                }

                String userId = auth.getUserId();
                if (userId == null) {
                    callback.onError("User ID not found. Please log in.");
                    return;
                }

                // Make the API call to get trips
                URL url = new URL(REST_URL + "/" + TRIPS_TABLE + "?user_id=eq." + userId + "&order=start_date.desc");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json");

                int responseCode = connection.getResponseCode();
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
                    Log.d(TAG, "Get trips response: " + responseStr);

                    // Parse trips
                    List<Trip> trips = parseTripsFromResponse(responseStr);
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
                    Log.e(TAG, "Get trips error: " + errorResponse);
                    callback.onError("Failed to get trips: " + errorResponse);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error getting trips: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Parse trips from JSON response
     */
    private List<Trip> parseTripsFromResponse(String jsonResponse) {
        List<Trip> trips = new ArrayList<>();
        try {
            JSONArray tripsArray = new JSONArray(jsonResponse);

            for (int i = 0; i < tripsArray.length(); i++) {
                JSONObject tripObj = tripsArray.getJSONObject(i);

                Trip trip = new Trip();
                trip.setTripId(tripObj.optString("trip_id", ""));
                trip.setUserId(tripObj.optString("user_id", ""));
                trip.setTripName(tripObj.optString("trip_name", ""));
                trip.setCountry(tripObj.optString("country", ""));
                trip.setJournal(tripObj.optString("journal", ""));

                // Parse dates
                SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                String startDateStr = tripObj.optString("start_date", "");
                String endDateStr = tripObj.optString("end_date", "");

                try {
                    if (!startDateStr.isEmpty()) {
                        trip.setStartDate(sqlDateFormat.parse(startDateStr));
                    }
                    if (!endDateStr.isEmpty()) {
                        trip.setEndDate(sqlDateFormat.parse(endDateStr));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing dates: " + e.getMessage());
                }

                // Parse image URLs
                JSONArray imageUrlsArray = tripObj.optJSONArray("image_url");
                if (imageUrlsArray != null) {
                    for (int j = 0; j < imageUrlsArray.length(); j++) {
                        String imageUrl = imageUrlsArray.optString(j, "");
                        if (!imageUrl.isEmpty()) {
                            trip.addImageUrl(imageUrl);
                        }
                    }
                }

                trips.add(trip);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing trips: " + e.getMessage(), e);
        }
        return trips;
    }

    /**
     * Upload a trip image to Supabase storage
     * @param imageData Image data as byte array
     * @param callback Callback to handle the response with image URL
     */
    public void uploadTripImage(byte[] imageData, TripCallback callback) {
        executor.execute(() -> {
            try {
                String accessToken = auth.getAccessToken();
                if (accessToken == null) {
                    callback.onError("Not authenticated. Please log in.");
                    return;
                }

                // Generate a unique filename
                String filename = "trip_" + System.currentTimeMillis() + ".jpg";

                // Upload the image to storage
                URL uploadUrl = new URL(STORAGE_URL + "/object/" + TRIP_IMAGES_BUCKET + "/" + filename);
                HttpsURLConnection uploadConn = (HttpsURLConnection) uploadUrl.openConnection();
                uploadConn.setRequestMethod("POST");
                uploadConn.setRequestProperty("apikey", API_KEY);
                uploadConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                uploadConn.setRequestProperty("Content-Type", "image/jpeg");
                uploadConn.setRequestProperty("x-upsert", "true");
                uploadConn.setDoOutput(true);

                try (OutputStream os = uploadConn.getOutputStream()) {
                    os.write(imageData);
                }

                int uploadResponseCode = uploadConn.getResponseCode();
                if (uploadResponseCode >= 200 && uploadResponseCode < 300) {
                    // Upload successful, return the URL
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + TRIP_IMAGES_BUCKET + "/" + filename;
                    Log.d(TAG, "Trip image uploaded successfully: " + publicUrl);
                    callback.onSuccess(publicUrl);
                } else {
                    // Upload failed
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(uploadConn.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Upload failed: " + errorResponse);
                    callback.onError("Failed to upload image: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading trip image: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Delete a trip from Supabase
     * @param tripId The ID of the trip to delete
     * @param callback Callback to handle the response
     */
    public void deleteTrip(String tripId, TripCallback callback) {
        executor.execute(() -> {
            try {
                String accessToken = auth.getAccessToken();
                if (accessToken == null) {
                    callback.onError("Not authenticated. Please log in.");
                    return;
                }

                // Make the API call to delete trip
                URL url = new URL(REST_URL + "/" + TRIPS_TABLE + "?trip_id=eq." + tripId);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    // Success
                    callback.onSuccess("Trip deleted successfully");
                } else {
                    // Error deleting trip
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Delete trip error: " + errorResponse);
                    callback.onError("Failed to delete trip: " + errorResponse);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error deleting trip: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }
}