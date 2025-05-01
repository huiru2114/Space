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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class SupabaseTrip {
    private static final String TAG = "SupabaseTrip";
    private static final String TRIPS_TABLE = "trips";
    private static final String TRIP_IMAGES_BUCKET = "trip_images";

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

                // Make the API call to create trip
                URL url = new URL(REST_URL + "/" + TRIPS_TABLE);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
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
                Log.d(TAG, "Trip creation response code: " + responseCode);

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
                    Log.d(TAG, "Trip creation response: " + responseStr);
                    callback.onSuccess("Trip created successfully");
                } else {
                    // Error creating trip
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Trip creation error response: " + errorResponse);
                    callback.onError("Failed to create trip: " + errorResponse);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error creating trip: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
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

                // Create bucket if it doesn't exist (similar to profile picture upload in SupabaseAuth)
                // Note: Your team may have already created this bucket

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
}