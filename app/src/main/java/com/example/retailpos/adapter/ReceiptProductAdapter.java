package com.example.retailpos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.R;
import com.example.retailpos.model.Product;

import java.util.List;

public class ReceiptProductAdapter extends RecyclerView.Adapter<ReceiptProductAdapter.ProductViewHolder> {

    private final List<Product> productList;

    public ReceiptProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchase_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.nameText.setText(product.name);
        holder.quantityText.setText("Qty: " + product.quantity);
        holder.priceText.setText("₱" + String.format("%.2f", product.price * product.quantity));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        TextView nameText, quantityText, priceText;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.productName);
            quantityText = itemView.findViewById(R.id.productQuantity);
            priceText = itemView.findViewById(R.id.productPrice);
        }
    }
}
