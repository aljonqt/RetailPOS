package com.example.retailpos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.*;

public class Purchase extends AppCompatActivity {

    private RecyclerView purchaseRecyclerView;
    private MaterialButton btnScan, btnBuyNow;
    private TextView tvTotal;
    private PurchaseAdapter purchaseAdapter;
    private List<Product> purchaseList = new ArrayList<>();
    private DatabaseReference productsRef;

    private final ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                IntentResult scanResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                if (scanResult != null && scanResult.getContents() != null) {
                    String scannedId = scanResult.getContents();

                    // Extract last segment if it's a URL
                    if (scannedId.contains("/")) {
                        scannedId = scannedId.substring(scannedId.lastIndexOf("/") + 1);
                    }

                    // Validate the Firebase key before use
                    if (isValidFirebaseKey(scannedId)) {
                        addProductToPurchase(scannedId);
                    } else {
                        Toast.makeText(this, "Invalid product code format", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No scan result", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase);

        purchaseRecyclerView = findViewById(R.id.purchaseRecyclerView);
        btnScan = findViewById(R.id.btnScan);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        tvTotal = findViewById(R.id.tvTotal);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        if (savedInstanceState != null) {
            purchaseList = savedInstanceState.getParcelableArrayList("purchaseList");
            if (purchaseList == null) purchaseList = new ArrayList<>();
        }

        purchaseAdapter = new PurchaseAdapter(purchaseList);
        purchaseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        purchaseRecyclerView.setAdapter(purchaseAdapter);

        productsRef = FirebaseDatabase.getInstance().getReference("products");

        btnScan.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(Purchase.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan Product Code");
            integrator.setBeepEnabled(true);
            integrator.setCameraId(0);
            integrator.setOrientationLocked(false);
            integrator.setBarcodeImageEnabled(false);
            scanLauncher.launch(integrator.createScanIntent());
        });

        btnBuyNow.setOnClickListener(v -> {
            if (purchaseList.isEmpty()) {
                Toast.makeText(Purchase.this, "No products selected", Toast.LENGTH_SHORT).show();
            } else {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                if (uid == null) {
                    Toast.makeText(Purchase.this, "User not logged in", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference cashiersRef = FirebaseDatabase.getInstance().getReference("cashiers");
                cashiersRef.child(uid).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String cashierName = snapshot.exists() ? snapshot.getValue(String.class) : "Unknown";
                        saveReceiptToFirebase(cashierName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Purchase.this, "Failed to retrieve cashier name", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        updateTotalPrice();
    }

    private void addProductToPurchase(String uid) {
        productsRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Product product = snapshot.getValue(Product.class);
                if (product != null) {
                    boolean exists = false;
                    for (Product p : purchaseList) {
                        if (p.uid.equals(product.uid)) {
                            p.quantity += 1;
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        product.quantity = 1;
                        purchaseList.add(product);
                    }
                    Collections.sort(purchaseList, (p1, p2) -> p1.name.compareToIgnoreCase(p2.name));
                    purchaseAdapter.notifyDataSetChanged();
                    updateTotalPrice();
                } else {
                    Toast.makeText(Purchase.this, "Product not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Purchase.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotalPrice() {
        double total = 0;
        for (Product product : purchaseList) {
            total += product.price * product.quantity;
        }
        tvTotal.setText("Total: ₱" + String.format("%.2f", total));
    }

    private void saveReceiptToFirebase(String cashierName) {
        DatabaseReference receiptsRef = FirebaseDatabase.getInstance().getReference("receipts");
        String receiptId = receiptsRef.push().getKey();

        if (receiptId == null) {
            Toast.makeText(this, "Failed to generate receipt ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        double total = 0;
        List<Map<String, Object>> productList = new ArrayList<>();
        for (Product product : purchaseList) {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("name", product.name);
            productMap.put("quantity", product.quantity);
            productMap.put("price", product.price);
            productList.add(productMap);
            total += product.price * product.quantity;
        }

        String cashierId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // ✅

        Map<String, Object> receiptData = new HashMap<>();
        receiptData.put("receiptId", receiptId);
        receiptData.put("cashierId", cashierId); // ✅ Add cashierId here
        receiptData.put("cashierName", cashierName);
        receiptData.put("dateTime", dateTime);
        receiptData.put("products", productList);
        receiptData.put("totalPrice", total);

        receiptsRef.child(receiptId).setValue(receiptData)
                .addOnSuccessListener(unused -> {
                    for (Product product : purchaseList) {
                        String productId = product.uid;
                        int purchasedQuantity = product.quantity;

                        DatabaseReference productRef = FirebaseDatabase.getInstance().getReference("products").child(productId);
                        productRef.child("quantity").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Long currentStock = snapshot.getValue(Long.class);
                                if (currentStock != null) {
                                    long newStock = currentStock - purchasedQuantity;
                                    productRef.child("quantity").setValue(Math.max(newStock, 0));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(Purchase.this, "Failed to update stock for " + product.name, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    Toast.makeText(Purchase.this, "Purchase completed", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Purchase.this, Receipt.class);
                    intent.putParcelableArrayListExtra("purchasedItems", new ArrayList<>(purchaseList));
                    startActivity(intent);

                    purchaseList.clear();
                    purchaseAdapter.notifyDataSetChanged();
                    updateTotalPrice();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Purchase.this, "Failed to save receipt", Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("purchaseList", new ArrayList<>(purchaseList));
    }

    private boolean isValidFirebaseKey(String key) {
        return key != null && !key.contains(".") && !key.contains("#") &&
                !key.contains("$") && !key.contains("[") &&
                !key.contains("]") && !key.contains("/");
    }
}
