package com.example.retailpos.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.R;
import com.example.retailpos.model.CashierSales;
import com.example.retailpos.model.Receipt;

import java.util.List;
import java.util.Locale;

public class CashierAdapter extends RecyclerView.Adapter<CashierAdapter.CashierViewHolder> {

    private Context context;
    private List<CashierSales> cashierList;
    private MonthAdapter.OnCashierClickListener listener;

    public CashierAdapter(Context context, List<CashierSales> cashierList, MonthAdapter.OnCashierClickListener listener) {
        this.context = context;
        this.cashierList = cashierList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CashierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sale, parent, false);
        return new CashierViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CashierViewHolder holder, int position) {
        CashierSales cashier = cashierList.get(position);
        holder.tvCashierName.setText(cashier.getCashierName());

        // Calculate total monthly sales
        double total = 0;
        List<Receipt> receipts = cashier.getReceipts();
        if (receipts != null) {
            for (Receipt receipt : receipts) {
                total += receipt.getTotalPrice();
            }
        }

        holder.tvTotalSales.setText("Total: ₱" + String.format(Locale.getDefault(), "%.2f", total));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCashierClick(receipts);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cashierList.size();
    }

    static class CashierViewHolder extends RecyclerView.ViewHolder {
        TextView tvCashierName, tvTotalSales;

        public CashierViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCashierName = itemView.findViewById(R.id.tvCashierName);
            tvTotalSales = itemView.findViewById(R.id.tvTotalSales); // Make sure this ID exists in item_sale.xml
        }
    }
}
