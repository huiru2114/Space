package com.example.space;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.view.MenuItem;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private LottieAnimationView animationBackground;
    private WebView cesiumWebView;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize animation view
        animationBackground = findViewById(R.id.animation_background);
        animationBackground.setScaleType(ImageView.ScaleType.CENTER_CROP);
        animationBackground.setSpeed(1.0f);
        animationBackground.enableMergePathsForKitKatAndAbove(true);
        animationBackground.playAnimation();

        // Initialize Cesium WebView
        cesiumWebView = findViewById(R.id.cesium_webview);
        setupCesiumWebView();

        // Initialize Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Set Home as default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            // Handle Home tab selection
            return true;
        } else if (itemId == R.id.nav_activity) {
            // Handle Activity tab selection
            return true;
        } else if (itemId == R.id.nav_explore) {
            // Handle Explore tab selection
            return true;
        } else if (itemId == R.id.nav_profile) {
            // Handle Profile tab selection
            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (animationBackground != null && !animationBackground.isAnimating()) {
            animationBackground.resumeAnimation();
        }
        cesiumWebView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (animationBackground != null && animationBackground.isAnimating()) {
            animationBackground.pauseAnimation();
        }
        cesiumWebView.onPause();
    }
}