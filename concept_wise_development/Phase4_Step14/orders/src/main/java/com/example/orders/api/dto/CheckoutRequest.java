package com.example.orders.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CheckoutRequest {

    @NotEmpty
    private List<Item> items;

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        @NotBlank
        private String sku;

        @Min(1)
        private int qty;

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
    }
}
