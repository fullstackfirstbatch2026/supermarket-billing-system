package com.example.supermarketbilling.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.supermarketbilling.entity.SaleItem;
import com.example.supermarketbilling.repository.SaleItemRepository;

@Service
public class SaleItemService {

    private final SaleItemRepository saleItemRepository;

    public SaleItemService(SaleItemRepository saleItemRepository) {
        this.saleItemRepository = saleItemRepository;
    }

    public List<SaleItem> getAllSaleItems() {
        return saleItemRepository.getAllSaleItems();
    }

    public List<SaleItem> getItemsBySaleId(Integer saleId) {
        return saleItemRepository.getItemsBySaleId(saleId);
    }

    public List<SaleItem> getItemsByProductId(Integer productId) {
        return saleItemRepository.getItemsByProductId(productId);
    }

    public Long getTotalQuantitySold(Integer productId) {
        return saleItemRepository.getTotalQuantitySold(productId);
    }

    public Double calculateInvoiceTotal(Integer saleId) {
        return saleItemRepository.calculateInvoiceTotalFromFunction(saleId);
    }

    public SaleItem saveSaleItem(SaleItem saleItem) {
        return saleItemRepository.save(saleItem);
    }

    public void deleteSaleItem(Integer id) {
        saleItemRepository.deleteById(id);
    }
}