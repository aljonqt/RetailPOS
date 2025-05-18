package com.example.retailpos.model;

public interface ReceiptListItem {
    int TYPE_HEADER = 0;
    int TYPE_ITEM = 1;

    int getType();
}
