package com.example.retailpos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditCashier extends BottomSheetDialogFragment {

    private TextInputEditText etName, etEmail, etNewPassword, etConfirmPassword;
    private MaterialButton btnUpdate;
    private String cashierId, currentName, currentEmail;

    public EditCashier(String cashierId, String name, String email) {
        this.cashierId = cashierId;
        this.currentName = name;
        this.currentEmail = email;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_cashier_bottom_sheet, container, false);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnUpdate = view.findViewById(R.id.btnUpdate);

        etName.setText(currentName);
        etEmail.setText(currentEmail);

        btnUpdate.setOnClickListener(v -> updateCashier());

        return view;
    }

    private void updateCashier() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(newPassword) && !newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cashierRef = FirebaseDatabase.getInstance()
                .getReference("cashiers").child(cashierId);

        cashierRef.child("name").setValue(name);
        cashierRef.child("email").setValue(email);

        if (!TextUtils.isEmpty(newPassword)) {
            cashierRef.child("password").setValue(newPassword);
        }

        Toast.makeText(getContext(), "Cashier updated successfully", Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
