package com.example.retailpos.model;

public class ReceiptModel {
    public String receiptId;
    public String dateTime;
    public String cashierName;
    public double totalPrice;
    public String name; // New field added for product name

    public ReceiptModel() {
        // Required for Firebase
    }

    public ReceiptModel(String receiptId, String dateTime, String cashierName, double totalPrice, String name) {
        this.receiptId = receiptId;
        this.dateTime = dateTime;
        this.cashierName = cashierName;
        this.totalPrice = totalPrice;
        this.name = name;
    }
}
