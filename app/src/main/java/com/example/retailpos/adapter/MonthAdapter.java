package com.example.retailpos.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.R;
import com.example.retailpos.model.CashierSales;
import com.example.retailpos.model.Receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.MonthViewHolder> {

    private Context context;
    private List<String> monthList;
    private Map<String, List<CashierSales>> groupedSales;
    private OnCashierClickListener listener;

    public interface OnCashierClickListener {
        void onCashierClick(List<Receipt> receipts);
    }

    public MonthAdapter(Context context, Map<String, List<CashierSales>> groupedSales, OnCashierClickListener listener) {
        this.context = context;
        this.groupedSales = groupedSales;
        this.listener = listener;
        this.monthList = new ArrayList<>(groupedSales.keySet());
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_month_header, parent, false);
        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        String month = monthList.get(position);
        holder.tvMonth.setText(month);

        List<CashierSales> cashierSalesList = groupedSales.get(month);
        CashierAdapter cashierAdapter = new CashierAdapter(context, cashierSalesList, listener);
        holder.recyclerCashiers.setLayoutManager(new LinearLayoutManager(context));
        holder.recyclerCashiers.setAdapter(cashierAdapter);
    }

    @Override
    public int getItemCount() {
        return monthList.size();
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth;
        RecyclerView recyclerCashiers;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.monthHeader);
            recyclerCashiers = itemView.findViewById(R.id.recyclerCashiers);
        }
    }
}
