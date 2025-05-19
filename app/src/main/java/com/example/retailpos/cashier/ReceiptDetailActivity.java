package com.example.retailpos.cashier;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.R;
import com.example.retailpos.adapter.ReceiptProductAdapter;
import com.example.retailpos.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReceiptDetailActivity extends AppCompatActivity {

    private TextView tvCashier, tvDateTime, tvTotal;
    private RecyclerView rvProducts;
    private MaterialButton btnDownload;

    private String receiptId;
    private String cashierName, dateTime;
    private int totalPrice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipt_detail);

        tvCashier = findViewById(R.id.tvCashierName);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvTotal = findViewById(R.id.tvTotalPrice);
        rvProducts = findViewById(R.id.rvPurchasedList);
        btnDownload = findViewById(R.id.btnDownload);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        receiptId = getIntent().getStringExtra("receiptId");
        if (receiptId != null) {
            loadReceiptDetails(receiptId);
        } else {
            Toast.makeText(this, "Receipt ID is missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadReceiptDetails(String receiptId) {
        DatabaseReference receiptRef = FirebaseDatabase.getInstance()
                .getReference("receipts")
                .child(receiptId);

        receiptRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ReceiptDetailActivity.this, "Receipt not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Default values in case fields are missing
                cashierName = snapshot.hasChild("cashierName") ? snapshot.child("cashierName").getValue(String.class) : "Unknown";
                dateTime = snapshot.hasChild("dateTime") ? snapshot.child("dateTime").getValue(String.class) : "Unknown";
                totalPrice = 0;

                List<Product> productList = new ArrayList<>();
                DataSnapshot productsSnapshot = snapshot.child("products");

                for (DataSnapshot productSnap : productsSnapshot.getChildren()) {
                    Map<String, Object> productMap = (Map<String, Object>) productSnap.getValue();
                    if (productMap != null) {
                        productList.add(parseProduct(productMap));
                    }
                }

                // Update UI
                tvCashier.setText("Cashier: " + cashierName);
                tvDateTime.setText("Date/Time: " + dateTime);
                tvTotal.setText(String.format(Locale.getDefault(), "Total: ₱%,d", totalPrice));
                rvProducts.setAdapter(new ReceiptProductAdapter(productList));

                // Set download PDF button
                btnDownload.setOnClickListener(v -> generatePdf(productList));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReceiptDetailActivity.this, "Failed to load receipt", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    private Product parseProduct(Map<String, Object> data) {
        String uid = data.get("uid") != null ? data.get("uid").toString() : null;
        String name = data.get("name") != null ? data.get("name").toString() : "Unnamed";

        int quantity = data.get("quantity") instanceof Number ? ((Number) data.get("quantity")).intValue() : 0;
        double price = data.get("price") instanceof Number ? ((Number) data.get("price")).doubleValue() : 0.0;
        int stock = data.get("stock") instanceof Number ? ((Number) data.get("stock")).intValue() : 0;
        long timestamp = data.get("timestamp") instanceof Number ? ((Number) data.get("timestamp")).longValue() : System.currentTimeMillis();

        totalPrice += quantity * price;

        Product product = new Product(uid, name, quantity, price, stock);
        product.timestamp = timestamp;

        return product;
    }


    private void safeDrawText(Canvas canvas, String text, float x, float y, Paint paint) {
        if (text != null && !text.trim().isEmpty()) {
            canvas.drawText(text, x, y, paint);
        }
    }

    private void generatePdf(List<Product> productList) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        int lineHeight = 20;
        int pageHeight = 200 + (productList.size() * lineHeight) + 60;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = 25;
        paint.setTextSize(14);
        safeDrawText(canvas, "Retail POS Receipt", 80, y, paint);
        y += 25;

        paint.setTextSize(12);
        safeDrawText(canvas, "Cashier: " + (cashierName != null ? cashierName : "N/A"), 10, y, paint);
        y += 18;
        safeDrawText(canvas, "Date/Time: " + (dateTime != null ? dateTime : "N/A"), 10, y, paint);
        y += 25;

        paint.setTextSize(11);
        safeDrawText(canvas, "Product", 10, y, paint);
        safeDrawText(canvas, "Qty", 130, y, paint);
        safeDrawText(canvas, "Price", 200, y, paint);
        y += 15;

        for (Product product : productList) {
            safeDrawText(canvas, product.name != null ? product.name : "Unnamed", 10, y, paint);
            safeDrawText(canvas, String.valueOf(product.quantity), 130, y, paint);
            safeDrawText(canvas, "₱" + String.format(Locale.getDefault(), "%.2f", product.price * product.quantity), 200, y, paint);
            y += 15;
        }

        y += 20;
        paint.setTextSize(12);
        safeDrawText(canvas, "Total: ₱" + totalPrice, 10, y, paint);

        pdfDocument.finishPage(page);

        String fileName = "receipt_" + System.currentTimeMillis() + ".pdf";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RetailPOS");

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        if (outputStream != null) {
                            pdfDocument.writeTo(outputStream);
                            Toast.makeText(this, "PDF saved to Downloads/RetailPOS", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RetailPOS");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    pdfDocument.writeTo(out);
                    Toast.makeText(this, "PDF saved to Downloads/RetailPOS", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }
}
