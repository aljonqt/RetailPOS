package com.example.retailpos.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.retailpos.R;
import com.example.retailpos.ReceiptDetailActivity;
import com.example.retailpos.adapter.ReceiptListAdapter;
import com.example.retailpos.model.ReceiptItem;
import com.example.retailpos.model.ReceiptHeader;
import com.example.retailpos.model.ReceiptListItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TransactionsFragment extends Fragment {

    private RecyclerView receiptRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ReceiptListAdapter adapter;
    private List<ReceiptListItem> receiptList = new ArrayList<>();
    private DatabaseReference receiptRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transactions_fragment, container, false);
        receiptRecyclerView = view.findViewById(R.id.receiptRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        adapter = new ReceiptListAdapter(receiptList, receiptItem -> {
            Intent intent = new Intent(getContext(), ReceiptDetailActivity.class);
            intent.putExtra("receiptId", receiptItem.getReceiptId());
            startActivity(intent);
        });

        receiptRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        receiptRecyclerView.setAdapter(adapter);

        receiptRef = FirebaseDatabase.getInstance().getReference("receipts");

        swipeRefreshLayout.setOnRefreshListener(this::loadReceipts);
        loadReceipts();

        return view;
    }

    private void loadReceipts() {
        swipeRefreshLayout.setRefreshing(true);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String currentCashierId = currentUser.getUid();

        receiptRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                receiptList.clear();

                Map<String, List<ReceiptItem>> groupedByMonth = new TreeMap<>(Collections.reverseOrder());
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                for (DataSnapshot receiptSnap : snapshot.getChildren()) {
                    String receiptId = receiptSnap.child("receiptId").getValue(String.class);
                    String dateTime = receiptSnap.child("dateTime").getValue(String.class);
                    String cashierId = receiptSnap.child("cashierId").getValue(String.class);

                    if (receiptId == null || dateTime == null || cashierId == null) continue;

                    if (!cashierId.equals(currentCashierId)) continue;

                    try {
                        Date parsedDate = fullFormat.parse(dateTime);
                        if (parsedDate == null) continue;

                        String month = monthFormat.format(parsedDate);
                        String date = dateFormat.format(parsedDate);
                        String time = timeFormat.format(parsedDate);

                        ReceiptItem item = new ReceiptItem(receiptId, date, time);

                        groupedByMonth
                                .computeIfAbsent(month, k -> new ArrayList<>())
                                .add(item);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                for (String month : groupedByMonth.keySet()) {
                    receiptList.add(new ReceiptHeader(month));
                    List<ReceiptItem> items = groupedByMonth.get(month);
                    Collections.sort(items, (a, b) -> b.getDate().compareTo(a.getDate()));
                    receiptList.addAll(items);
                }

                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}
