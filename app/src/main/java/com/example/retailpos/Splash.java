package com.example.retailpos;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        animateDots(); // Start dot loading animation

        // Delay before navigating to Login activity
        new Handler().postDelayed(() -> {
            startActivity(new Intent(Splash.this, Login.class));
            finish();
        }, SPLASH_DURATION);
    }

    private void animateDots() {
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        animateDot(dot1, 0);
        animateDot(dot2, 200);
        animateDot(dot3, 400);
    }

    private void animateDot(View dot, long delay) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f);
        animator.setDuration(600);
        animator.setStartDelay(delay);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }
}
