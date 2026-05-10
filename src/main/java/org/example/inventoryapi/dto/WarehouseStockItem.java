package org.example.inventoryapi.dto;

public interface WarehouseStockItem {
    int getProductId();
    String getProductName();
    String getSku();
    long getQuantity();
}
