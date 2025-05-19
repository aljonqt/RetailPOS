package com.example.retailpos.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.retailpos.R;
import com.example.retailpos.bottomsheet.EditProductBottomSheet;
import com.example.retailpos.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Inventory extends AppCompatActivity {

    private TableLayout productsTable;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.inventory);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        productsTable = findViewById(R.id.productsTable);
        databaseReference = FirebaseDatabase.getInstance().getReference("products");

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        loadProductsFromFirebase();
    }

    private void loadProductsFromFirebase() {
        databaseReference.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                productsTable.removeViews(1, productsTable.getChildCount() - 1);
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        addProductRow(product);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Inventory.this, "Failed to load products: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addProductRow(Product product) {
        TableRow row = new TableRow(this);
        row.setBackgroundResource(android.R.drawable.list_selector_background);

        TextView uidText = createTextCell(product.uid);
        TextView nameText = createTextCell(product.name);
        TextView stockText = createTextCell(String.valueOf(product.stock));
        TextView priceText = createTextCell("₱" + product.price);

        ImageButton editButton = new ImageButton(this);
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackgroundResource(android.R.color.transparent);
        editButton.setPadding(8, 8, 8, 8);
        editButton.setOnClickListener(v -> {
            EditProductBottomSheet bottomSheet = new EditProductBottomSheet(product);
            bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
        });

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setBackgroundResource(android.R.color.transparent);
        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(product));

        TableRow.LayoutParams buttonLayoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        TableRow actionsLayout = new TableRow(this);
        actionsLayout.addView(editButton, buttonLayoutParams);
        actionsLayout.addView(deleteButton, buttonLayoutParams);

        row.addView(uidText);
        row.addView(nameText);
        row.addView(stockText);
        row.addView(priceText);
        row.addView(actionsLayout);

        productsTable.addView(row);
    }

    private void showDeleteConfirmationDialog(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete \"" + product.name + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseReference.child(product.uid).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView createTextCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        tv.setGravity(View.TEXT_ALIGNMENT_CENTER);
        return tv;
    }
}
