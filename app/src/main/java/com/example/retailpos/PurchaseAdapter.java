package com.example.retailpos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.model.Product;

import java.util.List;

public class PurchaseAdapter extends RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder> {

    private final List<Product> productList;

    public PurchaseAdapter(List<Product> productList) {
        this.productList = productList;
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
        holder.quantity.setText("Qty: " + product.quantity);

        // Calculate total price based on quantity
        double totalPrice = product.price * product.quantity;
        holder.price.setText("₱" + String.format("%.2f", totalPrice));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class PurchaseViewHolder extends RecyclerView.ViewHolder {
        TextView name, quantity, price;

        public PurchaseViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            quantity = itemView.findViewById(R.id.productQuantity);
            price = itemView.findViewById(R.id.productPrice);
        }
    }
}
