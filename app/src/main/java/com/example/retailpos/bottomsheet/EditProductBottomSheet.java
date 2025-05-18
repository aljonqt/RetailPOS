package com.example.retailpos.bottomsheet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.retailpos.R;
import com.example.retailpos.model.Product;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditProductBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText nameInput, quantityInput, priceInput;
    private MaterialButton updateButton;
    private Product product;

    public EditProductBottomSheet(Product product) {
        this.product = product;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_product_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        nameInput = view.findViewById(R.id.editProductNameInput);
        quantityInput = view.findViewById(R.id.editProductQuantityInput);
        priceInput = view.findViewById(R.id.editProductPriceInput);
        updateButton = view.findViewById(R.id.updateProductButton);

        // Populate fields
        nameInput.setText(product.name);
        quantityInput.setText(String.valueOf(product.quantity));
        priceInput.setText(String.valueOf(product.price));

        updateButton.setOnClickListener(v -> updateProduct());
    }

    private void updateProduct() {
        String quantityStr = quantityInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();

        if (TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int newQuantity;
        double newPrice;
        try {
            newQuantity = Integer.parseInt(quantityStr);
            newPrice = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference productRef = FirebaseDatabase.getInstance()
                .getReference("products")
                .child(product.uid);

        productRef.child("quantity").setValue(newQuantity);
        productRef.child("price").setValue(newPrice)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Product updated", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
