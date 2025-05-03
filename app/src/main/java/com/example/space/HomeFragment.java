package com.example.space;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements AuthStateManager.AuthStateListener {

    private LottieAnimationView animationBackground;
    private WebView cesiumWebView;
    private SupabaseTrip supabaseTrip;
    private List<Trip> userTrips;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize SupabaseTrip to fetch user trips
        supabaseTrip = new SupabaseTrip(requireContext());

        // Initialize animation view
        animationBackground = view.findViewById(R.id.animation_background);
        animationBackground.setSpeed(1.0f);
        animationBackground.enableMergePathsForKitKatAndAbove(true);
        animationBackground.playAnimation();

        // Initialize Cesium WebView
        cesiumWebView = view.findViewById(R.id.cesium_webview);
        setupCesiumWebView();

        // Don't call loadUserTrips() here - it will be handled by the auth state listener

        return view;
    }

    private void setupCesiumWebView() {
        WebSettings webSettings = cesiumWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Enable console logging from JavaScript
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Allow mixed content (http content in https page)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Set WebView background transparent to show Lottie animation
        cesiumWebView.setBackgroundColor(0x00000000);
        cesiumWebView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

        // Add JavaScript interface to communicate between WebView and Java
        cesiumWebView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        // Load the HTML from assets folder
        cesiumWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Verify the interface is available by logging a message
                view.evaluateJavascript(
                        "console.log('Page loaded. AndroidInterface is ' + " +
                                "(typeof AndroidInterface !== 'undefined' ? 'available' : 'not available'));",
                        null
                );

                // If we have trip data, send it to the WebView
                if (userTrips != null && !userTrips.isEmpty()) {
                    sendTripsToWebView();
                }
            }
        });

        cesiumWebView.loadUrl("file:///android_asset/globe/webmap.html");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for auth state changes
        AuthStateManager.getInstance().addListener(this);

        if (animationBackground != null && !animationBackground.isAnimating()) {
            animationBackground.resumeAnimation();
        }
        cesiumWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister to prevent memory leaks
        AuthStateManager.getInstance().removeListener(this);

        if (animationBackground != null && animationBackground.isAnimating()) {
            animationBackground.pauseAnimation();
        }
        cesiumWebView.onPause();
    }

    @Override
    public void onAuthStateChanged(boolean isAuthenticated) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (isAuthenticated) {
                // User is authenticated, load trips
                loadUserTrips();
            } else {
                // User logged out, clear the globe
                userTrips = null;
                if (cesiumWebView != null) {
                    // Call JavaScript function to clear all pins
                    cesiumWebView.evaluateJavascript("clearAllPins();", null);
                }
            }
        });
    }

    /**
     * Load the user's trips from Supabase
     */
    private void loadUserTrips() {
        // Check authentication state first
        if (!AuthStateManager.getInstance().isAuthenticated()) {
            // Not authenticated, don't try to load trips
            return;
        }

        supabaseTrip.getUserTrips(new SupabaseTrip.TripDataCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                userTrips = trips;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Send trips to WebView if it's already loaded
                        if (cesiumWebView != null) {
                            sendTripsToWebView();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                System.out.println("Error loading trips: " + error);
            }
        });
    }

    /**
     * Send trip data to the WebView as JSON
     */
    private void sendTripsToWebView() {
        try {
            // Convert trips to JSON array
            JSONArray tripsArray = new JSONArray();
            SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            for (Trip trip : userTrips) {
                JSONObject tripObj = new JSONObject();
                tripObj.put("tripId", trip.getTripId());
                tripObj.put("tripName", trip.getTripName());
                tripObj.put("country", trip.getCountry());
                tripObj.put("journal", trip.getJournal());

                // Format dates
                if (trip.getStartDate() != null) {
                    tripObj.put("startDate", sqlDateFormat.format(trip.getStartDate()));
                }
                if (trip.getEndDate() != null) {
                    tripObj.put("endDate", sqlDateFormat.format(trip.getEndDate()));
                }

                // Add image URLs
                JSONArray imageUrlsArray = trip.getImageUrlsAsJsonArray();
                tripObj.put("imageUrls", imageUrlsArray);

                tripsArray.put(tripObj);
            }

            // Call JavaScript function to load trips
            final String tripsJson = tripsArray.toString();
            cesiumWebView.evaluateJavascript(
                    "loadTripPins(" + tripsJson + ");",
                    null
            );

            System.out.println("Sent " + userTrips.size() + " trips to WebView");
        } catch (JSONException e) {
            System.out.println("Error creating JSON for trips: " + e.getMessage());
        }
    }

    /**
     * JavaScript interface to handle communication between WebView and Java
     */
    private class WebAppInterface {

        @JavascriptInterface
        public void onCountrySelected(final String countryName) {
            if (getActivity() != null) {
                // Log message for debugging purposes
                System.out.println("onCountrySelected: " + countryName);

                // Must use runOnUiThread for UI operations (including starting activities)
                getActivity().runOnUiThread(() -> {
                    // Launch AddTripActivity with the selected country
                    Intent intent = new Intent(getActivity(), AddTripActivity.class);
                    intent.putExtra("selected_country", countryName);
                    startActivity(intent);
                });
            } else {
                System.out.println("Cannot launch AddTripActivity: getActivity() returned null");
            }
        }

        @JavascriptInterface
        public void onTripSelected(final String tripId) {
            if (getActivity() != null) {
                // Log message for debugging purposes
                System.out.println("onTripSelected: " + tripId);

                // Must use runOnUiThread for UI operations
                getActivity().runOnUiThread(() -> {
                    // Find the selected trip
                    Trip selectedTrip = null;
                    for (Trip trip : userTrips) {
                        if (trip.getTripId().equals(tripId)) {
                            selectedTrip = trip;
                            break;
                        }
                    }

                    if (selectedTrip != null) {
                        // Launch ViewTripActivity with the selected trip
                        Intent intent = new Intent(getActivity(), TripDetailActivity.class);
                        intent.putExtra("trip_id", tripId);
                        startActivity(intent);
                    } else {
                        System.out.println("Trip not found with ID: " + tripId);
                    }
                });
            } else {
                System.out.println("Cannot launch TripDetailActivity: getActivity() returned null");
            }
        }

        // This method can be called from JavaScript for debugging
        @JavascriptInterface
        public void showToast(final String message) {
            // Method kept for compatibility, but toasts are not shown anymore
            System.out.println("Debug message: " + message);
        }
    }
}