package com.example.retailpos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.retailpos.admin.AdminActivity;
import com.example.retailpos.cashier.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREF_NAME = "AdminPrefs";
    private static final String KEY_ADMIN_EMAIL = "admin_email";
    private static final String KEY_ADMIN_PASSWORD = "admin_password";

    public static String ADMIN_EMAIL = null;
    public static String ADMIN_PASSWORD = null;

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ✅ Restore saved admin credentials
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        ADMIN_EMAIL = prefs.getString(KEY_ADMIN_EMAIL, null);
        ADMIN_PASSWORD = prefs.getString(KEY_ADMIN_PASSWORD, null);

        // ✅ Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.getUid());
            return;
        }

        setContentView(R.layout.login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            ADMIN_EMAIL = email;
                            ADMIN_PASSWORD = password;

                            // ✅ Save credentials persistently
                            SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                            editor.putString(KEY_ADMIN_EMAIL, email);
                            editor.putString(KEY_ADMIN_PASSWORD, password);
                            editor.apply();

                            checkUserRoleAndNavigate(user.getUid());
                        } else {
                            Toast.makeText(Login.this, "User not found after login.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Login.this, "Authentication failed: " +
                                Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");

                            if ("admin".equals(role)) {
                                Toast.makeText(Login.this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Login.this, AdminActivity.class));
                            } else {
                                Toast.makeText(Login.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Login.this, MainActivity.class));
                            }
                        } else {
                            Toast.makeText(Login.this, "Cashier Login Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Login.this, MainActivity.class));
                        }
                        finish(); // ✅ Close login activity
                    } else {
                        Toast.makeText(Login.this, "Failed to retrieve user role.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
