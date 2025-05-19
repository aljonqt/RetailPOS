package com.example.retailpos.admin;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.R;
import com.example.retailpos.adapter.MonthAdapter;
import com.example.retailpos.model.CashierSales;
import com.example.retailpos.model.Receipt;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Sales extends AppCompatActivity {

    RecyclerView salesRecyclerView;
    MonthAdapter adapter;
    Map<String, List<CashierSales>> groupedSales = new LinkedHashMap<>();
    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    SimpleDateFormat outputMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Permission denied to write PDF", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sales);

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolBar); // Make sure your sales.xml uses this ID
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        salesRecyclerView = findViewById(R.id.salesRecyclerView);
        salesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchReceiptsFromFirebase();
    }


    private void fetchReceiptsFromFirebase() {
        FirebaseDatabase.getInstance().getReference("receipts")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Map<String, List<Receipt>>> tempMap = new LinkedHashMap<>();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String receiptId = ds.child("receiptId").getValue(String.class);
                            String cashierName = ds.child("cashierName").getValue(String.class);
                            String dateTime = ds.child("dateTime").getValue(String.class);
                            Double totalPrice = ds.child("totalPrice").getValue(Double.class);

                            if (receiptId == null || cashierName == null || dateTime == null || totalPrice == null)
                                continue;

                            String month;
                            try {
                                month = outputMonth.format(inputFormat.parse(dateTime));
                            } catch (ParseException e) {
                                continue;
                            }

                            Receipt receipt = new Receipt(receiptId, cashierName, dateTime, totalPrice);

                            tempMap.putIfAbsent(month, new LinkedHashMap<>());
                            Map<String, List<Receipt>> cashierMap = tempMap.get(month);
                            cashierMap.putIfAbsent(cashierName, new ArrayList<>());
                            cashierMap.get(cashierName).add(receipt);
                        }

                        for (String month : tempMap.keySet()) {
                            List<CashierSales> cashierSalesList = new ArrayList<>();
                            for (Map.Entry<String, List<Receipt>> entry : tempMap.get(month).entrySet()) {
                                cashierSalesList.add(new CashierSales(entry.getKey(), entry.getValue()));
                            }
                            groupedSales.put(month, cashierSalesList);
                        }

                        adapter = new MonthAdapter(Sales.this, groupedSales, Sales.this::showReceiptDialog);
                        salesRecyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void showReceiptDialog(List<Receipt> receipts) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) getResources().getDisplayMetrics().density * 16;
        layout.setPadding(padding, padding, padding, padding);

        final double[] totalMonthSales = {0};

        for (Receipt r : receipts) {
            TextView tvId = new TextView(this);
            tvId.setText("Receipt ID: " + r.getReceiptId());
            layout.addView(tvId);

            TextView tvDate = new TextView(this);
            tvDate.setText("Date: " + r.getDateTime());
            layout.addView(tvDate);

            TextView tvTotal = new TextView(this);
            tvTotal.setText("Total: ₱" + String.format(Locale.getDefault(), "%.2f", r.getTotalPrice()));
            tvTotal.setPadding(0, 0, 0, 24);
            layout.addView(tvTotal);

            totalMonthSales[0] += r.getTotalPrice();
        }

        TextView tvMonthTotal = new TextView(this);
        tvMonthTotal.setText("Monthly Total Sales: ₱" + String.format(Locale.getDefault(), "%.2f", totalMonthSales[0]));
        layout.addView(tvMonthTotal);

        Button downloadButton = new Button(this);
        downloadButton.setText("Download PDF");
        downloadButton.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        downloadButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                generatePdf(receipts, totalMonthSales[0]);
            }
        });
        layout.addView(downloadButton);

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setTitle("Receipts - " + receipts.get(0).getCashierName());
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void generatePdf(List<Receipt> receipts, double totalMonthSales) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        int pageHeight = 600 + receipts.size() * 70;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = 25;
        paint.setTextSize(12);

        String cashierName = receipts.get(0).getCashierName();
        canvas.drawText("Monthly Sales Report", 70, y, paint);
        y += 20;
        paint.setTextSize(10);
        canvas.drawText("Cashier: " + cashierName, 10, y, paint);
        y += 20;

        for (Receipt r : receipts) {
            canvas.drawText("Receipt ID: " + r.getReceiptId(), 10, y, paint);
            y += 15;
            canvas.drawText("Date: " + r.getDateTime(), 10, y, paint);
            y += 15;
            canvas.drawText("Total: ₱" + String.format(Locale.getDefault(), "%.2f", r.getTotalPrice()), 10, y, paint);
            y += 25;
        }

        paint.setTextSize(12);
        canvas.drawText("Monthly Total Sales: ₱" + String.format(Locale.getDefault(), "%.2f", totalMonthSales), 10, y, paint);

        pdfDocument.finishPage(page);

        String fileName = "sales_report_" + System.currentTimeMillis() + ".pdf";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
