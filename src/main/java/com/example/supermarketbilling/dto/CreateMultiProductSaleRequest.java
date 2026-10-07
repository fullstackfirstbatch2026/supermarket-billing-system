package com.example.supermarketbilling.dto;

import java.util.List;

public class CreateMultiProductSaleRequest {

    private Integer customerId;
    private List<SaleItemRequest> items;

    public CreateMultiProductSaleRequest() {
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public List<SaleItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SaleItemRequest> items) {
        this.items = items;
    }

    public static class SaleItemRequest {

        private Integer productId;
        private Integer quantity;

        public SaleItemRequest() {
        }

        public Integer getProductId() {
            return productId;
        }

        public void setProductId(Integer productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}