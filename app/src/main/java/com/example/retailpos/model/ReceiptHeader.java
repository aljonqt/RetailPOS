package com.example.retailpos.model;

public class ReceiptHeader implements ReceiptListItem {
    private final String month;

    public ReceiptHeader(String month) {
        this.month = month;
    }

    public String getMonth() {
        return month;
    }

    @Override
    public int getType() {
        return TYPE_HEADER;
    }
}
