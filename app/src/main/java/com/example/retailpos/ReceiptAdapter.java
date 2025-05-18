package com.example.retailpos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.model.Product;

import java.util.List;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ViewHolder> {

    private final List<Product> productList;

    public ReceiptAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ReceiptAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receipt_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.name);
        holder.tvQuantity.setText("x" + product.quantity);
        double total = product.price * product.quantity;
        holder.tvTotalPrice.setText("₱" + String.format("%.2f", total));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQuantity, tvTotalPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvQuantity = itemView.findViewById(R.id.tvProductQuantity);
            tvTotalPrice = itemView.findViewById(R.id.tvProductTotalPrice);
        }
    }
}
