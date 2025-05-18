package com.example.retailpos.model;

public class ReceiptItem implements ReceiptListItem {
    private final String receiptId;
    private final String date;
    private final String time;

    public ReceiptItem(String receiptId, String date, String time) {
        this.receiptId = receiptId;
        this.date = date;
        this.time = time;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    @Override
    public int getType() {
        return TYPE_ITEM;
    }
}
