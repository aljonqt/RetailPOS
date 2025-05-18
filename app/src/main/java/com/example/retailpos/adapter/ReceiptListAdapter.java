package com.example.retailpos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.retailpos.R;
import com.example.retailpos.model.ReceiptHeader;
import com.example.retailpos.model.ReceiptItem;
import com.example.retailpos.model.ReceiptListItem;

import java.util.List;

public class ReceiptListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ReceiptListItem> items;
    private final OnReceiptClickListener listener;

    public ReceiptListAdapter(List<ReceiptListItem> items, OnReceiptClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ReceiptListItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt, parent, false);
            return new ItemViewHolder(view, listener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ReceiptListItem item = items.get(position);
        if (item.getType() == ReceiptListItem.TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind((ReceiptHeader) item);
        } else {
            ((ItemViewHolder) holder).bind((ReceiptItem) item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthHeader = itemView.findViewById(R.id.tvMonthHeader);
        }

        void bind(ReceiptHeader header) {
            tvMonthHeader.setText(header.getMonth());
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView receiptIdText, dateText, timeText;
        private ReceiptItem currentItem;

        public ItemViewHolder(@NonNull View itemView, OnReceiptClickListener listener) {
            super(itemView);
            receiptIdText = itemView.findViewById(R.id.receiptIdText);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);

            itemView.setOnClickListener(v -> {
                if (listener != null && currentItem != null) {
                    listener.onReceiptClick(currentItem);
                }
            });
        }

        void bind(ReceiptItem item) {
            this.currentItem = item;
            receiptIdText.setText("ID: " + item.getReceiptId());
            dateText.setText("Date: " + item.getDate());
            timeText.setText("Time: " + item.getTime());
        }
    }

    // 🔑 Click listener interface
    public interface OnReceiptClickListener {
        void onReceiptClick(ReceiptItem receiptItem);
    }
}
