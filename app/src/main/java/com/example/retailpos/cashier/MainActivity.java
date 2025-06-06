package com.example.retailpos.cashier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.retailpos.Login;
import com.example.retailpos.R;
import com.example.retailpos.fragments.ProductsFragment;
import com.example.retailpos.fragments.TransactionsFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextView greetingText;
    private Button logoutButton;
    private ImageButton btnProducts, btnTransactions;
    private FloatingActionButton btnPurchase;
    private Switch themeSwitch;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    private SharedPreferences preferences;
    private static final String PREF_NAME = "theme_pref";
    private static final String KEY_NIGHT_MODE = "night_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before onCreate
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isNightMode = preferences.getBoolean(KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        greetingText = findViewById(R.id.greetingText);
        logoutButton = findViewById(R.id.logoutButton);
        btnProducts = findViewById(R.id.btnProducts);
        btnPurchase = findViewById(R.id.btnPurchase);
        btnTransactions = findViewById(R.id.btnTransactions);
        themeSwitch = findViewById(R.id.themeSwitch);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Set switch state
        themeSwitch.setChecked(isNightMode);

        // Fix blinking issue by restarting activity with no animation
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_NIGHT_MODE, isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

            // Restart activity without animation
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0); // No animation on finish
            startActivity(intent);
            overridePendingTransition(0, 0); // No animation on start
        });

        // Load greeting
        displayGreetingFromDatabase();

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(new ProductsFragment());
        }

        // Logout logic
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Navigation
        btnProducts.setOnClickListener(v -> loadFragment(new ProductsFragment()));
        btnPurchase.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Purchase.class)));
        btnTransactions.setOnClickListener(v -> loadFragment(new TransactionsFragment()));
    }

    private void displayGreetingFromDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            databaseRef.child("cashiers").child(uid).child("name").addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String cashierName = snapshot.getValue(String.class);
                            if (cashierName == null || cashierName.isEmpty()) {
                                cashierName = "Cashier";
                            }
                            greetingText.setText("Good Day, " + cashierName + "!!");
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            greetingText.setText("Good Day!!");
                            Toast.makeText(MainActivity.this, "Failed to load cashier name.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } else {
            greetingText.setText("Good Day!!");
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
}
