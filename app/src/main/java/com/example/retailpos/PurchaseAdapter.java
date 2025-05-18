package com.example.retailpos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.model.Product;

import java.util.List;

public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder> {

    private final List<Product> productList;
    private final OnQuantityChangeListener quantityChangeListener;
    private final Context context;

    public interface OnQuantityChangeListener {
        void onQuantityChanged(int position, int newQuantity);
    }

    public PurchaseAdapter(List<Product> productList, OnQuantityChangeListener listener, Context context) {
        this.productList = productList;
        this.quantityChangeListener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public PurchaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchase_product, parent, false);
        return new PurchaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchaseViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.name.setText(product.name);
        holder.quantity.setText(String.valueOf(product.quantity));

        // Calculate and show total price
        double totalPrice = product.price * product.quantity;
        holder.price.setText("₱" + String.format("%.2f", totalPrice));

        // Increase button
        holder.btnIncrease.setOnClickListener(v -> {
            if (product.quantity < product.stock) {
                product.quantity++;
                holder.quantity.setText(String.valueOf(product.quantity));
                updatePriceDisplay(holder, product);
                quantityChangeListener.onQuantityChanged(holder.getAdapterPosition(), product.quantity);
            } else {
                Toast.makeText(context,
                        "Only " + product.stock + " unit(s) available",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Decrease button
        holder.btnDecrease.setOnClickListener(v -> {
            if (product.quantity > 1) {
                product.quantity--;
                holder.quantity.setText(String.valueOf(product.quantity));
                updatePriceDisplay(holder, product);
                quantityChangeListener.onQuantityChanged(holder.getAdapterPosition(), product.quantity);
            }
        });
    }

    private void updatePriceDisplay(PurchaseViewHolder holder, Product product) {
        double updatedPrice = product.price * product.quantity;
        holder.price.setText("₱" + String.format("%.2f", updatedPrice));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class PurchaseViewHolder extends RecyclerView.ViewHolder {
        TextView name, quantity, price;
        ImageButton btnIncrease, btnDecrease;

        public PurchaseViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            quantity = itemView.findViewById(R.id.productQuantity);
            price = itemView.findViewById(R.id.productPrice);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
        }
    }
}
