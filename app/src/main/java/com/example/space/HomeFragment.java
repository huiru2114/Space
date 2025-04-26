package com.example.space;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        // Set WebView background transparent to show Lottie animation
        cesiumWebView.setBackgroundColor(0x00000000);
        cesiumWebView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

        // Load the HTML from assets folder
        cesiumWebView.setWebViewClient(new WebViewClient());
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
}