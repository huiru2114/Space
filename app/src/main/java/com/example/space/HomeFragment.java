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

public class HomeFragment extends Fragment {

    private LottieAnimationView animationBackground;
    private WebView cesiumWebView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize animation view
        animationBackground = view.findViewById(R.id.animation_background);
        animationBackground.setSpeed(1.0f);
        animationBackground.enableMergePathsForKitKatAndAbove(true);
        animationBackground.playAnimation();

        // Initialize Cesium WebView
        cesiumWebView = view.findViewById(R.id.cesium_webview);
        setupCesiumWebView();

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
            }
        });

        cesiumWebView.loadUrl("file:///android_asset/globe/webmap.html");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (animationBackground != null && !animationBackground.isAnimating()) {
            animationBackground.resumeAnimation();
        }
        cesiumWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (animationBackground != null && animationBackground.isAnimating()) {
            animationBackground.pauseAnimation();
        }
        cesiumWebView.onPause();
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

        // This method can be called from JavaScript for debugging
        @JavascriptInterface
        public void showToast(final String message) {
            // Method kept for compatibility, but toasts are not shown anymore
            System.out.println("Debug message: " + message);
        }
    }
}