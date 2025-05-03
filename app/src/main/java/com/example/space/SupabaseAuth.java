package com.example.space;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.FormBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class SupabaseAuth {
    private static final String TAG = "SupabaseAuth";

    private String SUPABASE_URL;
    private String API_KEY;
    private String AUTH_URL;
    private String REST_URL;
    private String STORAGE_URL;
    private static final String PROFILES_TABLE = "profiles";
    private static final String PROFILE_BUCKET = "avatars";

    private SharedPreferences authPrefs;
    private SharedPreferences signupPrefs;
    private ExecutorService executor;

    public boolean isAuthenticated() {
        String accessToken = getAccessToken();
        return accessToken != null && !accessToken.isEmpty();
    }

    // Constructor
    public SupabaseAuth(Context context) {
        this.authPrefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        this.signupPrefs = context.getSharedPreferences("SignupPrefs", Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();

        this.SUPABASE_URL = context.getResources().getString(R.string.SUPABASE_URL);
        this.API_KEY = context.getResources().getString(R.string.SUPABASE_KEY);
        this.AUTH_URL = SUPABASE_URL + "/auth/v1";
        this.REST_URL = SUPABASE_URL + "/rest/v1";
        this.STORAGE_URL = SUPABASE_URL + "/storage/v1";

        // Initialize auth state
        boolean isAuthenticated = isAuthenticated();
        AuthStateManager.getInstance().setAuthenticated(isAuthenticated);
    }

    /**
     * Interface for authentication callbacks
     */
    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Interface for profile data callbacks
     */
    public interface ProfileCallback {
        void onSuccess(String username, String phone, String profilePic);
        void onProfileNotFound();
        void onError(String error);
    }

    /**
     * Sign up a new user
     * @param email User email
     * @param password User password
     * @param callback Callback to handle the response
     */
    public void signUp(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting signup process for email: " + email);

                // Create JSON payload for signup
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("password", password);

                Log.d(TAG, "Sending signup request with payload: " + payload.toString());

                // Make the API call to signup
                URL url = new URL(AUTH_URL + "/signup");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Signup response code: " + responseCode);

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
                    Log.d(TAG, "Signup success response: " + responseStr);

                    JSONObject responseObj = new JSONObject(responseStr);
                    String userId = responseObj.optString("id", "");

                    if (!userId.isEmpty()) {
                        // Store the user ID with email-based key for future use
                        String userKey = email.replaceAll("[.@]", "_");

                        // Get pending data from signup preferences
                        String pendingUsername = signupPrefs.getString("pendingUsername_" + userKey, null);
                        String pendingPhone = signupPrefs.getString("pendingPhone_" + userKey, null);
                        String pendingProfilePic = signupPrefs.getString("pendingProfilePic_" + userKey, null);

                        Log.d(TAG, "Retrieved pending data - Username: " + pendingUsername +
                                ", Phone: " + pendingPhone +
                                ", ProfilePic: " + (pendingProfilePic != null ? "exists" : "null"));

                        // Store all user data in auth preferences
                        SharedPreferences.Editor editor = authPrefs.edit();
                        editor.putString("user_id_" + userKey, userId);
                        editor.putString("user_email_" + userId, email);
                        editor.putString("pendingUsername_" + userKey, pendingUsername);
                        editor.putString("pendingPhone_" + userKey, pendingPhone);
                        if (pendingProfilePic != null) {
                            editor.putString("pendingProfilePic_" + userKey, pendingProfilePic);
                        }
                        editor.apply();

                        Log.d(TAG, "User data stored successfully - User ID: " + userId);
                        callback.onSuccess("Signup successful. Please check your email for verification.");
                    } else {
                        Log.e(TAG, "Signup response missing user ID");
                        callback.onError("Signup successful but user ID is missing");
                    }
                } else {
                    // Error during signup
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Signup error response: " + errorResponse);

                    try {
                        JSONObject errorObj = new JSONObject(errorResponse);
                        String errorMsg = errorObj.optString("message", "Signup failed");
                        String errorCode = errorObj.optString("error", "");

                        // Provide more specific error messages
                        if (errorCode.equals("email_already_exists")) {
                            errorMsg = "This email is already registered";
                        } else if (errorCode.equals("weak_password")) {
                            errorMsg = "Password is too weak";
                        } else if (errorCode.equals("invalid_email")) {
                            errorMsg = "Invalid email format";
                        }

                        Log.e(TAG, "Signup error: " + errorMsg);
                        callback.onError(errorMsg);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing error response: " + e.getMessage());
                        callback.onError("Signup failed: " + errorResponse);
                    }
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error during signup: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Sign in an existing user
     * @param email User email
     * @param password User password
     * @param callback Callback to handle the response
     */
    public void signIn(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                // Create JSON payload for login
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("password", password);

                // Make the API call to login
                URL url = new URL(AUTH_URL + "/token?grant_type=password");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

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

                    JSONObject responseObj = new JSONObject(response.toString());
                    String accessToken = responseObj.optString("access_token", "");
                    String refreshToken = responseObj.optString("refresh_token", "");

                    if (!accessToken.isEmpty()) {
                        // Get user information from the token
                        String userId = getUserIdFromToken(accessToken);
                        Log.d(TAG, "Login successful - User ID: " + userId);

                        // Create email-based key for this user
                        String userKey = email.replaceAll("[.@]", "_");

                        // Store the tokens and user ID with user-specific keys
                        SharedPreferences.Editor editor = authPrefs.edit();
                        editor.putString("access_token_" + userKey, accessToken);
                        editor.putString("refresh_token_" + userKey, refreshToken);
                        editor.putString("user_id_" + userKey, userId);
                        editor.putString("user_email_" + userId, email);
                        editor.putString("current_user_email", email); // Store current active user
                        editor.putString("current_user_id", userId);   // Store current active user ID
                        editor.apply();

                        Log.d(TAG, "User data stored successfully");

                        // Update authentication state - this will notify all listeners
                        AuthStateManager.getInstance().setAuthenticated(true);

                        callback.onSuccess("Login successful");
                    } else {
                        Log.e(TAG, "Invalid login response - No access token");
                        callback.onError("Invalid login response");
                    }
                } else {
                    // Error during login
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject errorObj = new JSONObject(response.toString());
                    String errorMsg = errorObj.optString("message", "Login failed");
                    Log.e(TAG, "Login error: " + errorMsg);
                    callback.onError(errorMsg);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error during login: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Extract user ID from JWT token
     * @param token JWT token
     * @return User ID
     */
    private String getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";

            String payload = parts[1];
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            JSONObject payloadObj = new JSONObject(decodedPayload);
            return payloadObj.optString("sub", "");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing token: " + e.getMessage());
            return "";
        }
    }

    /**
     * Sign out the current user
     */
    public void signOut() {
        String currentEmail = authPrefs.getString("current_user_email", null);

        if (currentEmail != null) {
            // Only clear current user's data (not all users)
            String userKey = currentEmail.replaceAll("[.@]", "_");
            authPrefs.edit()
                    .remove("access_token_" + userKey)
                    .remove("refresh_token_" + userKey)
                    .remove("current_user_email")
                    .remove("current_user_id")
                    .apply();
        } else {
            // For backward compatibility, still clear these
            authPrefs.edit()
                    .remove("access_token")
                    .remove("refresh_token")
                    .remove("user_id")
                    .apply();
        }

        // Update auth state - this will notify all listeners
        AuthStateManager.getInstance().setAuthenticated(false);
    }

    /**
     * Get the current user ID
     * @return User ID or null if not logged in
     */
    public String getUserId() {
        String currentUserId = authPrefs.getString("current_user_id", null);
        if (currentUserId != null) {
            return currentUserId;
        }

        // Backward compatibility
        return authPrefs.getString("user_id", null);
    }

    /**
     * Get the current user email
     * @return User email or null if not logged in
     */
    public String getCurrentUserEmail() {
        return authPrefs.getString("current_user_email", null);
    }

    /**
     * Get the current access token
     * @return Access token or null if not logged in
     */
    public String getAccessToken() {
        String currentEmail = authPrefs.getString("current_user_email", null);

        if (currentEmail != null) {
            String userKey = currentEmail.replaceAll("[.@]", "_");
            return authPrefs.getString("access_token_" + userKey, null);
        }

        // Backward compatibility
        return authPrefs.getString("access_token", null);
    }

    /**
     * Fetch user profile from Supabase
     * @param userId User ID to fetch profile for
     * @param callback Callback to handle the response
     */
    public void fetchProfile(String userId, ProfileCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Fetching profile for user: " + userId);
                String accessToken = getAccessToken();
                if (accessToken == null) {
                    Log.e(TAG, "No access token found");
                    callback.onError("Not authenticated");
                    return;
                }

                // Make the API call to fetch profile
                URL url = new URL(REST_URL + "/" + PROFILES_TABLE + "?id=eq." + userId);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Prefer", "return=representation");
                connection.setRequestProperty("X-Client-Info", "supabase-android/1.0.0");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Profile fetch response code: " + responseCode);

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
                    Log.d(TAG, "Profile fetch response: " + responseStr);

                    if (responseStr.isEmpty() || responseStr.equals("[]")) {
                        Log.d(TAG, "Profile not found");
                        callback.onProfileNotFound();
                        return;
                    }

                    // Extract the profile data
                    JSONObject profileData = new JSONObject(responseStr.substring(1, responseStr.length() - 1));
                    String username = profileData.optString("username", "");
                    String phone = profileData.optString("phone", "");
                    String profilePic = profileData.optString("profile_pic", "");

                    Log.d(TAG, "Profile data retrieved - Username: " + username + ", Phone: " + phone + ", ProfilePic: " + profilePic);

                    // Save to preferences with user-specific keys
                    String userEmail = authPrefs.getString("user_email_" + userId, null);
                    if (userEmail != null) {
                        String userKey = userEmail.replaceAll("[.@]", "_");
                        SharedPreferences.Editor editor = authPrefs.edit();
                        editor.putString("username_" + userKey, username);
                        editor.putString("phone_" + userKey, phone);
                        editor.putString("profilePic_" + userKey, profilePic);
                        editor.apply();
                        Log.d(TAG, "Profile data saved to preferences");
                    }

                    callback.onSuccess(username, phone, profilePic);
                } else {
                    // Error fetching profile
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Profile fetch error response: " + errorResponse);

                    JSONObject errorObj = new JSONObject(errorResponse);
                    String errorMsg = errorObj.optString("message", "Failed to fetch profile");
                    callback.onError(errorMsg);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error fetching profile: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Create a profile for a user
     * @param userId User ID to create profile for
     * @param username Username for the profile
     * @param phone Phone number (optional)
     * @param callback Callback to handle the response
     */
    public void createProfile(String userId, String username, String phone, AuthCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting profile creation for user: " + userId);
                String accessToken = getAccessToken();
                if (accessToken == null) {
                    Log.e(TAG, "No access token found");
                    callback.onError("Not authenticated");
                    return;
                }

                // Get the user's email to retrieve signup data
                String userEmail = authPrefs.getString("user_email_" + userId, null);
                if (userEmail == null) {
                    Log.e(TAG, "User email not found for userId: " + userId);
                    callback.onError("User email not found");
                    return;
                }

                Log.d(TAG, "Found user email: " + userEmail);
                String userKey = userEmail.replaceAll("[.@]", "_");
                Log.d(TAG, "Using user key: " + userKey);

                // Get pending data from signup preferences
                String pendingUsername = signupPrefs.getString("pendingUsername_" + userKey, null);
                String pendingPhone = signupPrefs.getString("pendingPhone_" + userKey, null);
                String pendingProfilePic = signupPrefs.getString("pendingProfilePic_" + userKey, null);

                Log.d(TAG, "Retrieved pending data - Username: " + pendingUsername +
                        ", Phone: " + pendingPhone +
                        ", ProfilePic: " + (pendingProfilePic != null ? "exists" : "null"));

                // Validate username
                if (pendingUsername == null || pendingUsername.trim().isEmpty()) {
                    Log.e(TAG, "Username is required but not found in pending data");
                    callback.onError("Username is required");
                    return;
                }

                // Create JSON payload for creating profile
                JSONObject payload = new JSONObject();
                payload.put("id", userId);
                payload.put("username", pendingUsername.trim());  // Ensure username is trimmed
                payload.put("phone", pendingPhone != null ? pendingPhone.trim() : "");
                payload.put("created_at", "now()");
                payload.put("updated_at", "now()");

                Log.d(TAG, "Sending profile creation request with payload: " + payload.toString());

                // Check if we have a pending profile picture for this user
                if (pendingProfilePic != null && !pendingProfilePic.isEmpty()) {
                    Log.d(TAG, "Found pending profile picture");

                    // Upload the profile picture directly to the existing bucket
                    String profilePicUrl = uploadProfilePicture(userId, pendingProfilePic, accessToken);
                    if (profilePicUrl != null) {
                        payload.put("profile_pic", profilePicUrl);
                    }
                }

                // Make the API call to create profile
                URL url = new URL(REST_URL + "/" + PROFILES_TABLE + "?id=eq." + userId);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("PATCH");
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
                Log.d(TAG, "Profile creation response code: " + responseCode);

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
                    Log.d(TAG, "Profile creation response: " + responseStr);

                    if (!responseStr.isEmpty() && !responseStr.equals("[]")) {
                        // Extract the profile data and save to preferences
                        JSONObject profileData = new JSONObject(responseStr.substring(1, responseStr.length() - 1));

                        SharedPreferences.Editor editor = authPrefs.edit();
                        editor.putString("username_" + userKey, profileData.optString("username", ""));
                        editor.putString("phone_" + userKey, profileData.optString("phone", ""));
                        editor.putString("profilePic_" + userKey, profileData.optString("profile_pic", ""));
                        editor.apply();

                        Log.d(TAG, "Profile data saved to preferences");

                        // Clean up the signup preferences for this user
                        signupPrefs.edit()
                                .remove("pendingProfilePic_" + userKey)
                                .remove("pendingUsername_" + userKey)
                                .remove("pendingPhone_" + userKey)
                                .remove("userEmail_" + userKey)
                                .apply();

                        Log.d(TAG, "Signup preferences cleaned up");
                    }

                    callback.onSuccess("Profile created successfully");
                } else {
                    // Error creating profile
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String errorResponse = response.toString();
                    Log.e(TAG, "Profile creation error response: " + errorResponse);

                    JSONObject errorObj = new JSONObject(errorResponse);
                    String errorMsg = errorObj.optString("message", "Failed to create profile");
                    callback.onError(errorMsg);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error creating profile: " + e.getMessage(), e);
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Upload a profile picture to Supabase storage
     * @param userId User ID for the filename
     * @param base64Image Base64 encoded image
     * @param accessToken Access token for authentication
     * @return URL of the uploaded image or null if upload failed
     */
    private String uploadProfilePicture(String userId, String base64Image, String accessToken) {
        try {
            // Generate a unique filename with timestamp to avoid conflicts
            String filename = userId + "_" + System.currentTimeMillis() + ".jpg";

            // Decode base64 image
            byte[] imageData = Base64.decode(base64Image, Base64.DEFAULT);

            // First, check if bucket exists
            URL bucketUrl = new URL(STORAGE_URL + "/bucket/" + PROFILE_BUCKET);
            HttpsURLConnection bucketConn = (HttpsURLConnection) bucketUrl.openConnection();
            bucketConn.setRequestMethod("HEAD");
            bucketConn.setRequestProperty("apikey", API_KEY);
            bucketConn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int bucketResponseCode = bucketConn.getResponseCode();
            bucketConn.disconnect();

            if (bucketResponseCode == 404) {
                // First, check if we have permission to create buckets
                URL checkPermUrl = new URL(STORAGE_URL + "/bucket");
                HttpsURLConnection checkPermConn = (HttpsURLConnection) checkPermUrl.openConnection();
                checkPermConn.setRequestMethod("GET");
                checkPermConn.setRequestProperty("apikey", API_KEY);
                checkPermConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                checkPermConn.setRequestProperty("x-client-info", "supabase-android/1.0.0");

                int permResponseCode = checkPermConn.getResponseCode();
                if (permResponseCode >= 400) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(checkPermConn.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.e(TAG, "No permission to create buckets: " + response.toString());
                    checkPermConn.disconnect();
                    return null;
                }
                checkPermConn.disconnect();

                // Create bucket with proper permissions
                Log.d(TAG, "Creating bucket: " + PROFILE_BUCKET);
                URL createBucketUrl = new URL(STORAGE_URL + "/bucket");
                HttpsURLConnection createBucketConn = (HttpsURLConnection) createBucketUrl.openConnection();
                createBucketConn.setRequestMethod("POST");
                createBucketConn.setRequestProperty("apikey", API_KEY);
                createBucketConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                createBucketConn.setRequestProperty("Content-Type", "application/json");
                createBucketConn.setRequestProperty("x-client-info", "supabase-android/1.0.0");
                createBucketConn.setDoOutput(true);

                JSONObject bucketPayload = new JSONObject();
                bucketPayload.put("id", PROFILE_BUCKET);
                bucketPayload.put("name", "User Avatars");
                bucketPayload.put("public", true);
                bucketPayload.put("file_size_limit", 5242880);
                bucketPayload.put("allowed_mime_types", new JSONArray().put("image/jpeg").put("image/png"));

                try (OutputStream os = createBucketConn.getOutputStream()) {
                    byte[] input = bucketPayload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int createBucketResponseCode = createBucketConn.getResponseCode();
                if (createBucketResponseCode >= 400) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(createBucketConn.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.e(TAG, "Failed to create bucket: " + response.toString());
                    createBucketConn.disconnect();
                    return null;
                }
                createBucketConn.disconnect();
                Log.d(TAG, "Bucket created successfully");

                // Wait for bucket to be ready
                Thread.sleep(2000);

                // Verify bucket exists
                URL verifyBucketUrl = new URL(STORAGE_URL + "/bucket/" + PROFILE_BUCKET);
                HttpsURLConnection verifyConn = (HttpsURLConnection) verifyBucketUrl.openConnection();
                verifyConn.setRequestMethod("HEAD");
                verifyConn.setRequestProperty("apikey", API_KEY);
                verifyConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                verifyConn.setRequestProperty("x-client-info", "supabase-android/1.0.0");

                int verifyResponseCode = verifyConn.getResponseCode();
                verifyConn.disconnect();

                if (verifyResponseCode != 200) {
                    Log.e(TAG, "Bucket verification failed: " + verifyResponseCode);
                    return null;
                }
                Log.d(TAG, "Bucket verified successfully");

                // Set bucket policies
                URL policyUrl = new URL(STORAGE_URL + "/bucket/" + PROFILE_BUCKET + "/policy");
                HttpsURLConnection policyConn = (HttpsURLConnection) policyUrl.openConnection();
                policyConn.setRequestMethod("POST");
                policyConn.setRequestProperty("apikey", API_KEY);
                policyConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                policyConn.setRequestProperty("Content-Type", "application/json");
                policyConn.setRequestProperty("x-client-info", "supabase-android/1.0.0");
                policyConn.setDoOutput(true);

                JSONObject policyPayload = new JSONObject();
                policyPayload.put("action", "upload");
                policyPayload.put("effect", "allow");
                policyPayload.put("principal", "*");
                policyPayload.put("resource", PROFILE_BUCKET + "/*");

                try (OutputStream os = policyConn.getOutputStream()) {
                    byte[] input = policyPayload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int policyResponseCode = policyConn.getResponseCode();
                if (policyResponseCode >= 400) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(policyConn.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.e(TAG, "Failed to set bucket policy: " + response.toString());
                    policyConn.disconnect();
                    return null;
                }
                policyConn.disconnect();
                Log.d(TAG, "Bucket policy set successfully");
            }

            // Now upload the file
            URL uploadUrl = new URL(STORAGE_URL + "/object/" + PROFILE_BUCKET + "/" + filename);
            HttpsURLConnection uploadConn = (HttpsURLConnection) uploadUrl.openConnection();
            uploadConn.setRequestMethod("POST");
            uploadConn.setRequestProperty("apikey", API_KEY);
            uploadConn.setRequestProperty("Authorization", "Bearer " + accessToken);
            uploadConn.setRequestProperty("Content-Type", "image/jpeg");
            uploadConn.setRequestProperty("x-upsert", "true"); // Allow overwriting existing files
            uploadConn.setDoOutput(true);

            try (OutputStream os = uploadConn.getOutputStream()) {
                os.write(imageData);
            }

            int uploadResponseCode = uploadConn.getResponseCode();
            if (uploadResponseCode >= 200 && uploadResponseCode < 300) {
                // Upload successful, return the URL
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(uploadConn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Construct the public URL
                String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + PROFILE_BUCKET + "/" + filename;
                Log.d(TAG, "Profile picture uploaded successfully: " + publicUrl);
                return publicUrl;
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
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error uploading profile picture: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Update user profile
     * @param userId User ID
     * @param username New username (optional)
     * @param phone New phone number (optional)
     * @param profilePic Base64 encoded profile picture (optional)
     * @param callback Callback to handle the response
     */
    public void updateProfile(String userId, String username, String phone, String profilePic, AuthCallback callback) {
        executor.execute(() -> {
            try {
                String accessToken = getAccessToken();
                if (accessToken == null) {
                    callback.onError("Not authenticated");
                    return;
                }

                // Get user email for this userId
                String userEmail = authPrefs.getString("user_email_" + userId, null);
                String userKey = userEmail != null ? userEmail.replaceAll("[.@]", "_") : null;

                // Create JSON payload for updating profile
                JSONObject payload = new JSONObject();

                if (username != null && !username.isEmpty()) {
                    payload.put("username", username);
                }

                if (phone != null) {
                    payload.put("phone", phone);
                }

                if (profilePic != null && !profilePic.isEmpty()) {
                    String profilePicUrl = uploadProfilePicture(userId, profilePic, accessToken);
                    if (profilePicUrl != null) {
                        payload.put("profile_pic", profilePicUrl);
                    }
                }

                // Always update the updated_at timestamp
                payload.put("updated_at", "now()");

                // If nothing to update, return success
                if (payload.length() == 0) {
                    callback.onSuccess("No changes to update");
                    return;
                }

                // Make the API call to update profile
                URL url = new URL(REST_URL + "/" + PROFILES_TABLE + "?id=eq." + userId);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("PATCH");
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
                    if (!responseStr.isEmpty() && !responseStr.equals("[]") && userKey != null) {
                        // Extract the profile data and save to preferences
                        JSONObject profileData = new JSONObject(responseStr.substring(1, responseStr.length() - 1));

                        SharedPreferences.Editor editor = authPrefs.edit();
                        editor.putString("username_" + userKey, profileData.optString("username", ""));
                        editor.putString("phone_" + userKey, profileData.optString("phone", ""));
                        editor.putString("profilePic_" + userKey, profileData.optString("profile_pic", ""));
                        editor.apply();
                    }

                    callback.onSuccess("Profile updated successfully");
                } else {
                    // Error updating profile
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject errorObj = new JSONObject(response.toString());
                    String errorMsg = errorObj.optString("message", "Failed to update profile");
                    callback.onError(errorMsg);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error updating profile: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Check if the user's access token is valid
     * @param callback Callback to handle the response
     */
    public void validateSession(AuthCallback callback) {
        executor.execute(() -> {
            try {
                String accessToken = getAccessToken();
                if (accessToken == null) {
                    callback.onError("Not authenticated");
                    return;
                }

                // Make the API call to check auth status
                URL url = new URL(AUTH_URL + "/user");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", API_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    // Token is valid
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject userObj = new JSONObject(response.toString());
                    String userId = userObj.optString("id", "");
                    String email = userObj.optString("email", "");

                    // Update the user ID and email in preferences
                    if (!userId.isEmpty() && !email.isEmpty()) {
                        String userKey = email.replaceAll("[.@]", "_");
                        SharedPreferences.Editor editor = authPrefs.edit();
                        editor.putString("user_id_" + userKey, userId);
                        editor.putString("user_email_" + userId, email);
                        editor.putString("current_user_id", userId);
                        editor.putString("current_user_email", email);
                        editor.apply();
                    }

                    callback.onSuccess("Session valid");
                } else {
                    // Token is invalid, clear auth data
                    signOut();
                    callback.onError("Session expired");
                }

                connection.disconnect();

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error validating session: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Retrieves the pending profile data for a user after signup
     * @param email The email address used during signup
     * @return Object array containing [username, phone, profilePic] or null if not found
     */
    public Object[] getPendingProfileData(String email) {
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "Email is null or empty in getPendingProfileData");
            return null;
        }

        String userKey = email.replaceAll("[.@]", "_");
        Log.d(TAG, "Retrieving pending data for user: " + userKey);

        // First try to get data from auth preferences
        String username = authPrefs.getString("pendingUsername_" + userKey, null);
        String phone = authPrefs.getString("pendingPhone_" + userKey, null);
        String profilePic = authPrefs.getString("pendingProfilePic_" + userKey, null);

        // If not found in auth preferences, try signup preferences
        if (username == null) {
            username = signupPrefs.getString("pendingUsername_" + userKey, null);
            phone = signupPrefs.getString("pendingPhone_" + userKey, null);
            profilePic = signupPrefs.getString("pendingProfilePic_" + userKey, null);
        }

        if (username == null) {
            Log.d(TAG, "No pending data found for user: " + userKey);
            return null;
        }

        Log.d(TAG, "Found pending data - Username: " + username +
                ", Phone: " + phone +
                ", ProfilePic: " + (profilePic != null ? "exists" : "null"));

        return new Object[]{username, phone, profilePic};
    }

    /**
     * Change the user's password
     * @param currentPassword Current password for verification
     * @param newPassword New password to set
     * @param callback Callback to handle the response
     */
    public void changePassword(String currentPassword, String newPassword, AuthCallback callback) {
        executor.execute(() -> {
            try {
                // Get the user's email and access token
                String email = getCurrentUserEmail();
                String accessToken = getAccessToken();

                if (email == null || accessToken == null) {
                    callback.onError("Not authenticated");
                    return;
                }

                // First, verify the current password by attempting to sign in
                URL signInUrl = new URL(AUTH_URL + "/token?grant_type=password");
                HttpsURLConnection signInConn = (HttpsURLConnection) signInUrl.openConnection();
                signInConn.setRequestMethod("POST");
                signInConn.setRequestProperty("apikey", API_KEY);
                signInConn.setRequestProperty("Content-Type", "application/json");
                signInConn.setDoOutput(true);

                // Create sign-in payload
                JSONObject signInPayload = new JSONObject();
                signInPayload.put("email", email);
                signInPayload.put("password", currentPassword);

                try (OutputStream os = signInConn.getOutputStream()) {
                    byte[] input = signInPayload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int signInResponseCode = signInConn.getResponseCode();
                if (signInResponseCode < 200 || signInResponseCode >= 300) {
                    // Current password is incorrect
                    callback.onError("Current password is incorrect");
                    signInConn.disconnect();
                    return;
                }
                signInConn.disconnect();

                // Current password verified, now update to new password
                URL updateUrl = new URL(AUTH_URL + "/user");
                HttpsURLConnection updateConn = (HttpsURLConnection) updateUrl.openConnection();
                updateConn.setRequestMethod("PUT");
                updateConn.setRequestProperty("apikey", API_KEY);
                updateConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                updateConn.setRequestProperty("Content-Type", "application/json");
                updateConn.setDoOutput(true);

                // Create update payload
                JSONObject updatePayload = new JSONObject();
                updatePayload.put("password", newPassword);

                try (OutputStream os = updateConn.getOutputStream()) {
                    byte[] input = updatePayload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int updateResponseCode = updateConn.getResponseCode();
                if (updateResponseCode >= 200 && updateResponseCode < 300) {
                    // Password updated successfully
                    callback.onSuccess("Password changed successfully");
                } else {
                    // Error updating password
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(updateConn.getErrorStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject errorObj = new JSONObject(response.toString());
                    String errorMsg = errorObj.optString("message", "Failed to change password");
                    callback.onError(errorMsg);
                }

                updateConn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error changing password: " + e.getMessage());
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    public boolean refreshToken() {
        String currentEmail = authPrefs.getString("current_user_email", null);
        if (currentEmail == null) {
            Log.e(TAG, "No current user email found for token refresh");
            return false;
        }

        String userKey = currentEmail.replaceAll("[.@]", "_");
        String refreshToken = authPrefs.getString("refresh_token_" + userKey, null);
        if (refreshToken == null) {
            Log.e(TAG, "No refresh token found for current user");
            return false;
        }

        try {
            // Create JSON payload for token refresh
            JSONObject payload = new JSONObject();
            payload.put("refresh_token", refreshToken);

            // Make the API call to refresh token
            URL url = new URL(AUTH_URL + "/token?grant_type=refresh_token");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("apikey", API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

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

                JSONObject responseObj = new JSONObject(response.toString());
                String newAccessToken = responseObj.optString("access_token", "");
                String newRefreshToken = responseObj.optString("refresh_token", "");

                if (!newAccessToken.isEmpty()) {
                    // Store the new tokens
                    SharedPreferences.Editor editor = authPrefs.edit();
                    editor.putString("access_token_" + userKey, newAccessToken);
                    if (!newRefreshToken.isEmpty()) {
                        editor.putString("refresh_token_" + userKey, newRefreshToken);
                    }
                    editor.apply();

                    Log.d(TAG, "Token refreshed successfully");
                    return true;
                } else {
                    Log.e(TAG, "Invalid refresh token response - No access token");
                    return false;
                }
            } else {
                // Error during token refresh
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject errorObj = new JSONObject(response.toString());
                String errorMsg = errorObj.optString("message", "Token refresh failed");
                Log.e(TAG, "Token refresh error: " + errorMsg);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during token refresh: " + e.getMessage(), e);
            return false;
        }
    }
}
