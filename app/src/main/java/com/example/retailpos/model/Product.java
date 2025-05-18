package com.example.retailpos.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable {
    public String uid;
    public String name;
    public int quantity;
    public double price;
    public long timestamp;
    public int stock; // ✅ New field

    // Default constructor for Firebase
    public Product() {
        this.timestamp = System.currentTimeMillis();
    }

    // Full constructor with UID and stock
    public Product(String uid, String name, int quantity, double price, int stock) {
        this.uid = uid;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.stock = stock;
        this.timestamp = System.currentTimeMillis();
    }

    // Lightweight constructor for receipt display (stock not needed)
    public Product(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }

    // Parcelable implementation
    protected Product(Parcel in) {
        uid = in.readString();
        name = in.readString();
        quantity = in.readInt();
        price = in.readDouble();
        timestamp = in.readLong();
        stock = in.readInt(); // ✅ Read stock
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uid);
        parcel.writeString(name);
        parcel.writeInt(quantity);
        parcel.writeDouble(price);
        parcel.writeLong(timestamp);
        parcel.writeInt(stock); // ✅ Write stock
    }
}
