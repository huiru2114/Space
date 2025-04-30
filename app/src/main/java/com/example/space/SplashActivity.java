package com.example.space;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.core.splashscreen.SplashScreen;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3500; // 3.5 seconds
    private LottieAnimationView animationView;
    private TextView appNameText;
    private TextView taglineText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Must be called BEFORE super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        // Prevent default splash screen from disappearing too quickly
        splashScreen.setKeepOnScreenCondition(() -> false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        // Initialize views
        animationView = findViewById(R.id.animation_view);
        appNameText = findViewById(R.id.app_name_text);
        taglineText = findViewById(R.id.tagline_text);

        // Start animations with sequence
        startAnimationSequence();

        // Navigate to MainActivity after delay
        new Handler().postDelayed(this::navigateToMainActivity, SPLASH_DELAY);
    }

    private void startAnimationSequence() {
        // First make sure the text views are invisible
        appNameText.setAlpha(0f);
        taglineText.setAlpha(0f);

        // Animation for app name
        ObjectAnimator appNameFadeIn = ObjectAnimator.ofFloat(appNameText, "alpha", 0f, 1f);
        appNameFadeIn.setDuration(800);
        appNameFadeIn.setStartDelay(1000); // Start after 1 second

        // Animation for tagline
        ObjectAnimator taglineFadeIn = ObjectAnimator.ofFloat(taglineText, "alpha", 0f, 1f);
        taglineFadeIn.setDuration(800);
        taglineFadeIn.setStartDelay(1200); // Start slightly after app name

        // Scale animation for Lottie view
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(animationView, "scaleX", 0.8f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(animationView, "scaleY", 0.8f, 1.1f, 1.0f);

        scaleX.setDuration(1500);
        scaleY.setDuration(1500);

        // Play the animations together
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(appNameFadeIn, taglineFadeIn, scaleX, scaleY);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);

        // Add a nice transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Close splash activity
        finish();
    }
}