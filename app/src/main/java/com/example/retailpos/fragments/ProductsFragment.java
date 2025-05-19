package com.example.retailpos.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.retailpos.R;
import com.example.retailpos.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProductsFragment extends Fragment {

    private TableLayout productsTable;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseReference productsRef;

    public ProductsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.products_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        productsTable = view.findViewById(R.id.productsTable);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        int orange = ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark);
        swipeRefreshLayout.setColorSchemeColors(orange);

        productsRef = FirebaseDatabase.getInstance().getReference("products");

        loadProducts();

        swipeRefreshLayout.setOnRefreshListener(this::loadProducts);
    }

    private void loadProducts() {
        swipeRefreshLayout.setRefreshing(true);

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                Context context = getContext();
                if (context == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                productsTable.removeViews(1, Math.max(0, productsTable.getChildCount() - 1));

                List<Product> productList = new ArrayList<>();

                for (DataSnapshot productSnap : snapshot.getChildren()) {
                    Product product = productSnap.getValue(Product.class);
                    if (product != null) {
                        product.uid = productSnap.getKey();
                        productList.add(product);
                    }
                }

                Collections.sort(productList, Comparator.comparing(p -> p.name != null ? p.name.toLowerCase() : ""));

                for (Product product : productList) {
                    TableRow row = new TableRow(context);

                    row.addView(createCell(context, product.uid));
                    row.addView(createCell(context, product.name));
                    row.addView(createCell(context, String.valueOf(product.stock)));
                    row.addView(createCell(context, "₱" + String.format("%.2f", product.price)));

                    productsTable.addView(row);
                }

                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private TextView createCell(Context context, String text) {
        TextView cell = new TextView(context);
        cell.setText(text);
        cell.setPadding(8, 8, 8, 8);
        cell.setGravity(Gravity.CENTER);
        return cell;
    }
}
