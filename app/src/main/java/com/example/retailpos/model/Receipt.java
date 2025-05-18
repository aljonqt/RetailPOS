package com.example.retailpos.model;

public class Receipt {
    private String receiptId;
    private String cashierName;
    private String dateTime;
    private double totalPrice;

    // Required no-argument constructor for Firebase
    public Receipt() {}

    // Constructor
    public Receipt(String receiptId, String cashierName, String dateTime, double totalPrice) {
        this.receiptId = receiptId;
        this.cashierName = cashierName;
        this.dateTime = dateTime;
        this.totalPrice = totalPrice;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
