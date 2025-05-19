package com.example.retailpos.cashier;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.R;
import com.example.retailpos.adapter.ReceiptAdapter;
import com.example.retailpos.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class Receipt extends AppCompatActivity {

    private TextView tvCashierName, tvDateTime, tvTotalPrice;
    private RecyclerView rvPurchasedList;
    private MaterialButton btnDone, btnDownload;
    private List<Product> purchasedItems;
    private double totalPrice = 0.0;
    private String cashierName = "Cashier";
    private String dateTime;

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    generatePdf();
                } else {
                    Toast.makeText(this, "Permission denied to write PDF", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.retailpos.R.layout.receipt);

        // UI initialization
        tvCashierName = findViewById(com.example.retailpos.R.id.tvCashierName);
        tvDateTime = findViewById(com.example.retailpos.R.id.tvDateTime);
        tvTotalPrice = findViewById(com.example.retailpos.R.id.tvTotalPrice);
        rvPurchasedList = findViewById(com.example.retailpos.R.id.rvPurchasedList);
        btnDone = findViewById(com.example.retailpos.R.id.btnDone);
        btnDownload = findViewById(R.id.btnDownload);

        btnDownload.setEnabled(false); // Disable until data is ready

        // Current date and time
        dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        tvDateTime.setText("Date/Time: " + dateTime);

        // Get purchased items
        purchasedItems = getIntent().getParcelableArrayListExtra("purchasedItems");
        if (purchasedItems == null) purchasedItems = new ArrayList<>();

        for (Product product : purchasedItems) {
            totalPrice += product.price * product.quantity;
        }

        tvTotalPrice.setText("Total: ₱" + String.format("%.2f", totalPrice));

        // RecyclerView
        ReceiptAdapter adapter = new ReceiptAdapter(purchasedItems);
        rvPurchasedList.setLayoutManager(new LinearLayoutManager(this));
        rvPurchasedList.setAdapter(adapter);

        // Load current cashier name
        loadCashierName();

        // Button actions
        btnDone.setOnClickListener(v -> finish());

        btnDownload.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                generatePdf();
            }
        });
    }

    private void loadCashierName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference cashierRef = FirebaseDatabase.getInstance()
                    .getReference("cashiers")
                    .child(uid)
                    .child("name");

            cashierRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.getValue(String.class);
                    if (name != null && !name.isEmpty()) {
                        cashierName = name;
                    }
                    tvCashierName.setText("Cashier: " + cashierName);
                    btnDownload.setEnabled(true);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvCashierName.setText("Cashier: Unknown");
                    btnDownload.setEnabled(true);
                    Toast.makeText(Receipt.this, "Failed to load cashier name", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            tvCashierName.setText("Cashier: Unknown");
            btnDownload.setEnabled(true);
        }
    }

    private void generatePdf() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600 + purchasedItems.size() * 30, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = 25;
        paint.setTextSize(12);
        canvas.drawText("Mobile POS Receipt", 80, y, paint);
        y += 20;
        canvas.drawText("Cashier: " + cashierName, 10, y, paint);
        y += 20;
        canvas.drawText("Date/Time: " + dateTime, 10, y, paint);
        y += 30;

        paint.setTextSize(10);
        canvas.drawText("Item", 10, y, paint);
        canvas.drawText("Qty", 130, y, paint);
        canvas.drawText("Price", 200, y, paint);
        y += 15;

        for (Product product : purchasedItems) {
            canvas.drawText(product.name, 10, y, paint);
            canvas.drawText(String.valueOf(product.quantity), 130, y, paint);
            canvas.drawText("₱" + String.format("%.2f", product.price * product.quantity), 200, y, paint);
            y += 15;
        }

        y += 20;
        paint.setTextSize(12);
        canvas.drawText("Total: ₱" + String.format("%.2f", totalPrice), 10, y, paint);

        pdfDocument.finishPage(page);

        String fileName = "receipt_" + System.currentTimeMillis() + ".pdf";

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RetailPOS");

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        pdfDocument.writeTo(outputStream);
                        outputStream.close();
                        Toast.makeText(this, "PDF saved to Downloads/RetailPOS", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RetailPOS");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                FileOutputStream out = new FileOutputStream(file);
                pdfDocument.writeTo(out);
                out.close();
                Toast.makeText(this, "PDF saved to Downloads/RetailPOS", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }
}
