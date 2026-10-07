package com.example.supermarketbilling.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.supermarketbilling.entity.SaleItem;
import com.example.supermarketbilling.service.SaleItemService;

@RestController
@RequestMapping("/api/sale-items")
@CrossOrigin(origins = "*")
public class SaleItemController {

    private final SaleItemService saleItemService;

    public SaleItemController(SaleItemService saleItemService) {
        this.saleItemService = saleItemService;
    }

    @GetMapping
    public List<SaleItem> getAllSaleItems() {
        return saleItemService.getAllSaleItems();
    }

    @GetMapping("/sale/{saleId}")
    public List<SaleItem> getItemsBySaleId(@PathVariable Integer saleId) {
        return saleItemService.getItemsBySaleId(saleId);
    }

    @GetMapping("/product/{productId}")
    public List<SaleItem> getItemsByProductId(
            @PathVariable Integer productId) {
        return saleItemService.getItemsByProductId(productId);
    }

    @GetMapping("/product/{productId}/quantity-sold")
    public Long getTotalQuantitySold(@PathVariable Integer productId) {
        return saleItemService.getTotalQuantitySold(productId);
    }

    @GetMapping("/sale/{saleId}/invoice-total")
    public Double getInvoiceTotal(@PathVariable Integer saleId) {
        return saleItemService.calculateInvoiceTotal(saleId);
    }

    @PostMapping
    public SaleItem addSaleItem(@RequestBody SaleItem saleItem) {
        saleItem.setSaleItemId(null);
        return saleItemService.saveSaleItem(saleItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSaleItem(@PathVariable Integer id) {
        saleItemService.deleteSaleItem(id);
        return ResponseEntity.noContent().build();
    }
}