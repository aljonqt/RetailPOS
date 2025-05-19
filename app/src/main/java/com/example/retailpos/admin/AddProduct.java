package com.example.retailpos.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.retailpos.R;
import com.example.retailpos.model.Product;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class AddProduct extends BottomSheetDialogFragment {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private TextView productUidText;
    private TextInputEditText productNameInput, productStockInput, productPriceInput;
    private MaterialButton scanButton, saveProductButton;

    private DatabaseReference databaseReference;
    private String scannedBarcode = "";
    private IntentIntegrator intentIntegrator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_product, container, false);

        databaseReference = FirebaseDatabase.getInstance().getReference("products");

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        productUidText = view.findViewById(R.id.productUidText);
        productNameInput = view.findViewById(R.id.productNameInput);
        productStockInput = view.findViewById(R.id.productStockInput); // renamed from productQuantityInput
        productPriceInput = view.findViewById(R.id.productPriceInput);
        scanButton = view.findViewById(R.id.scanButton);
        saveProductButton = view.findViewById(R.id.saveProductButton);
    }

    private void setupClickListeners() {
        scanButton.setOnClickListener(v -> checkCameraPermission());
        saveProductButton.setOnClickListener(v -> saveProductToDatabase());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startBarcodeScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScanner();
            } else {
                showToast("Camera permission is required to scan barcodes");
            }
        }
    }

    private void startBarcodeScanner() {
        intentIntegrator = IntentIntegrator.forSupportFragment(this);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                .setPrompt("Scan a barcode")
                .setCameraId(0)
                .setBeepEnabled(true)
                .setBarcodeImageEnabled(true)
                .initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                showToast("Scan cancelled");
            } else {
                handleSuccessfulScan(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleSuccessfulScan(String barcode) {
        scannedBarcode = barcode;
        productUidText.setText(getString(R.string.scanned_uid_format, scannedBarcode));
        productNameInput.requestFocus();
    }

    private void saveProductToDatabase() {
        if (!validateInputs()) return;

        Product product = new Product(
                scannedBarcode,
                productNameInput.getText().toString().trim(),
                0, // quantity used in receipt only
                Double.parseDouble(productPriceInput.getText().toString().trim()),
                Integer.parseInt(productStockInput.getText().toString().trim()) // stock
        );

        saveProductToFirebase(product);
    }

    private boolean validateInputs() {
        if (scannedBarcode.isEmpty()) {
            showToast("Please scan a product barcode first");
            return false;
        }

        if (productNameInput.getText().toString().trim().isEmpty()) {
            productNameInput.setError("Product name is required");
            productNameInput.requestFocus();
            return false;
        }

        if (productStockInput.getText().toString().trim().isEmpty()) {
            productStockInput.setError("Stock is required");
            productStockInput.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(productStockInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            productStockInput.setError("Please enter a valid stock amount");
            productStockInput.requestFocus();
            return false;
        }

        if (productPriceInput.getText().toString().trim().isEmpty()) {
            productPriceInput.setError("Price is required");
            productPriceInput.requestFocus();
            return false;
        }

        try {
            Double.parseDouble(productPriceInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            productPriceInput.setError("Please enter a valid price");
            productPriceInput.requestFocus();
            return false;
        }

        return true;
    }

    private void saveProductToFirebase(Product product) {
        databaseReference.child(scannedBarcode).setValue(product)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Product saved successfully");
                        clearForm();
                        dismiss(); // Close bottom sheet
                    } else {
                        showToast("Failed to save product: " +
                                task.getException().getMessage());
                    }
                });
    }

    private void clearForm() {
        scannedBarcode = "";
        productUidText.setText(R.string.default_product_uid_text);
        productNameInput.setText("");
        productStockInput.setText("");
        productPriceInput.setText("");
        productNameInput.clearFocus();
        productStockInput.clearFocus();
        productPriceInput.clearFocus();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
