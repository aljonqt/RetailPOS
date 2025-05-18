package com.example.retailpos.model;

import java.util.List;

public class CashierSales {
    private String cashierName;
    private List<Receipt> receipts;

    // ✅ Required for Firebase or object mapping libraries
    public CashierSales() {}

    // ✅ Constructor for manual creation
    public CashierSales(String cashierName, List<Receipt> receipts) {
        this.cashierName = cashierName;
        this.receipts = receipts;
    }

    // ✅ Getters
    public String getCashierName() {
        return cashierName;
    }

    public List<Receipt> getReceipts() {
        return receipts;
    }

    // ✅ Setters
    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public void setReceipts(List<Receipt> receipts) {
        this.receipts = receipts;
    }
}
