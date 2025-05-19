package com.example.retailpos.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.retailpos.Login;
import com.example.retailpos.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminActivity extends AppCompatActivity {

    private static final String PREF_NAME = "AdminPrefs";
    private static final String KEY_ADMIN_EMAIL = "admin_email";
    private static final String KEY_ADMIN_PASSWORD = "admin_password";
    private static final String KEY_NIGHT_MODE = "night_mode";

    private MaterialButton logoutButton, inventoryButton, addProductButton, addCashierButton, salesButton;
    private TableLayout cashiersTable;
    private DatabaseReference cashierRef, receiptRef;
    private ValueEventListener cashierListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseAuth mAuth;
    private BarChart barChart;
    private Switch themeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Load theme setting before super.onCreate
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isNightMode = preferences.getBoolean(KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Init views
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        logoutButton = findViewById(R.id.logoutButton);
        inventoryButton = findViewById(R.id.inventoryButton);
        addProductButton = findViewById(R.id.addProductButton);
        addCashierButton = findViewById(R.id.addCashierButton);
        salesButton = findViewById(R.id.salesButton);
        cashiersTable = findViewById(R.id.cashiersTable);
        barChart = findViewById(R.id.barChart);
        themeSwitch = findViewById(R.id.themeSwitch); // Ensure you added this in XML

        // Setup theme switch state
        themeSwitch.setChecked(isNightMode);
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_NIGHT_MODE, isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

            // Restart activity to apply theme
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Firebase
        cashierRef = FirebaseDatabase.getInstance().getReference("cashiers");
        receiptRef = FirebaseDatabase.getInstance().getReference("receipts");

        int orangeColor = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
        swipeRefreshLayout.setColorSchemeColors(orangeColor);

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            clearAdminCredentials();
            redirectToLogin();
        });

        inventoryButton.setOnClickListener(v -> startActivity(new Intent(this, Inventory.class)));

        addProductButton.setOnClickListener(v -> {
            AddProduct addProduct = new AddProduct();
            addProduct.show(getSupportFragmentManager(), "AddProductBottomSheet");
        });

        salesButton.setOnClickListener(v -> startActivity(new Intent(this, Sales.class)));

        addCashierButton.setOnClickListener(v -> {
            String email = Login.ADMIN_EMAIL;
            String password = Login.ADMIN_PASSWORD;

            if (email == null || password == null) {
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                email = prefs.getString(KEY_ADMIN_EMAIL, null);
                password = prefs.getString(KEY_ADMIN_PASSWORD, null);
            }

            if (email != null && password != null) {
                AddCashier addCashier = new AddCashier();
                addCashier.setAdminCredentials(email, password);
                addCashier.show(getSupportFragmentManager(), "AddCashierBottomSheet");
            } else {
                Toast.makeText(this, "Admin credentials not available", Toast.LENGTH_SHORT).show();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadCashiers();
            loadDailySalesChart();
        });

        loadCashiers();
        loadDailySalesChart();
    }

    private void loadCashiers() {
        if (cashierListener != null) {
            cashierRef.removeEventListener(cashierListener);
        }

        swipeRefreshLayout.setRefreshing(true);

        cashierListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                swipeRefreshLayout.setRefreshing(false);
                cashiersTable.removeViews(1, Math.max(0, cashiersTable.getChildCount() - 1));

                if (!snapshot.exists()) {
                    Toast.makeText(AdminActivity.this, "No cashiers found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    String email = child.child("email").getValue(String.class);
                    addCashierRow(id, name, email);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(AdminActivity.this, "Failed to load cashiers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        cashierRef.addValueEventListener(cashierListener);
    }

    private void loadDailySalesChart() {
        receiptRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Float> dailySales = new TreeMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String dateTime = child.child("dateTime").getValue(String.class);
                    Double totalPrice = child.child("totalPrice").getValue(Double.class);
                    if (dateTime != null && totalPrice != null) {
                        try {
                            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateTime);
                            String dayKey = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(date);
                            dailySales.put(dayKey, dailySales.getOrDefault(dayKey, 0f) + totalPrice.floatValue());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                int index = 0;
                for (Map.Entry<String, Float> entry : dailySales.entrySet()) {
                    entries.add(new BarEntry(index, entry.getValue()));
                    labels.add(entry.getKey());
                    index++;
                }

                BarDataSet dataSet = new BarDataSet(entries, "Daily Sales");
                dataSet.setColor(Color.parseColor("#FFA500")); // Orange/Yellow
                BarData barData = new BarData(dataSet);

                barData.setBarWidth(0.4f); // ✅ Thinner bar (adjust between 0.2f - 0.5f as needed)

                barChart.setData(barData);
                barChart.getDescription().setEnabled(false);
                barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                barChart.getXAxis().setGranularity(1f);
                barChart.getXAxis().setGranularityEnabled(true);
                barChart.getAxisLeft().setAxisMinimum(0f);
                barChart.getAxisRight().setEnabled(false);
                barChart.setFitBars(true);
                barChart.invalidate(); // Refresh chart
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load sales chart: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCashierRow(String id, String name, String email) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView idView = new TextView(this);
        idView.setText(id);
        idView.setPadding(8, 8, 8, 8);
        row.addView(idView);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setPadding(8, 8, 8, 8);
        row.addView(nameView);

        TextView emailView = new TextView(this);
        emailView.setText(email);
        emailView.setPadding(8, 8, 8, 8);
        row.addView(emailView);

        View actionView = LayoutInflater.from(this).inflate(com.example.retailpos.R.layout.cashier_actions, null);
        ImageButton editBtn = actionView.findViewById(com.example.retailpos.R.id.editCashierBtn);
        ImageButton deleteBtn = actionView.findViewById(R.id.deleteCashierBtn);

        editBtn.setOnClickListener(v -> {
            EditCashier editCashier = new EditCashier(id, name, email);
            editCashier.show(getSupportFragmentManager(), "EditCashierBottomSheet");
        });

        deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Cashier")
                    .setMessage("Are you sure you want to delete this cashier?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        cashierRef.child(id).removeValue().addOnSuccessListener(aVoid -> {
                            loadCashiers();
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        row.addView(actionView);
        cashiersTable.addView(row);
    }

    private void clearAdminCredentials() {
        Login.ADMIN_EMAIL = null;
        Login.ADMIN_PASSWORD = null;

        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.remove(KEY_ADMIN_EMAIL);
        editor.remove(KEY_ADMIN_PASSWORD);
        editor.apply();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(AdminActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cashierListener != null) {
            cashierRef.removeEventListener(cashierListener);
        }
    }
}