package com.example.retailpos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.retailpos.model.Cashier;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddCashier extends BottomSheetDialogFragment {

    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference cashiersRef;
    private CashierAddedListener cashierAddedListener;

    // Admin credentials passed from caller
    private String adminEmail;
    private String adminPassword;

    public interface CashierAddedListener {
        void onCashierAdded();
    }

    public void setCashierAddedListener(CashierAddedListener listener) {
        this.cashierAddedListener = listener;
    }

    // Call this before showing the dialog
    public void setAdminCredentials(String email, String password) {
        this.adminEmail = email;
        this.adminPassword = password;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_cashier, container, false);

        mAuth = FirebaseAuth.getInstance();
        cashiersRef = FirebaseDatabase.getInstance().getReference("cashiers");

        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        view.findViewById(R.id.btnAddCashier).setOnClickListener(v -> addCashier());

        return view;
    }

    private void addCashier() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (adminEmail == null || adminPassword == null) {
            Toast.makeText(getContext(), "Admin credentials not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create cashier account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Cashier cashier = new Cashier(userId, fullName, email, "cashier");

                        cashiersRef.child(userId).setValue(cashier)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        // Sign out the new cashier
                                        mAuth.signOut();

                                        // Sign back in as admin
                                        mAuth.signInWithEmailAndPassword(adminEmail, adminPassword)
                                                .addOnCompleteListener(adminLogin -> {
                                                    if (adminLogin.isSuccessful()) {
                                                        Toast.makeText(getContext(), "Cashier added", Toast.LENGTH_SHORT).show();
                                                        if (cashierAddedListener != null) {
                                                            cashierAddedListener.onCashierAdded();
                                                        }
                                                        dismiss();
                                                    } else {
                                                        Toast.makeText(getContext(), "Cashier added, but failed to log back in as admin", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(getContext(), "Failed to save cashier in database", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
