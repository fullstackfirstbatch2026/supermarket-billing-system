package com.example.supermarketbilling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.example.supermarketbilling.entity.SaleItem;

public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {

    // 1. Get all sale items
    @Query(value = "SELECT * FROM SALE_ITEMS", nativeQuery = true)
    List<SaleItem> getAllSaleItems();

    // 2. Get sale items by sale ID
    @Query(value = "SELECT * FROM SALE_ITEMS WHERE sale_id = :saleId",
           nativeQuery = true)
    List<SaleItem> getItemsBySaleId(@Param("saleId") Integer saleId);

    // 3. Get sale items by product ID
    @Query(value = "SELECT * FROM SALE_ITEMS WHERE product_id = :productId",
           nativeQuery = true)
    List<SaleItem> getItemsByProductId(@Param("productId") Integer productId);

    // 4. Calculate total quantity sold for a product
    @Query(value = """
            SELECT COALESCE(SUM(quantity), 0)
            FROM SALE_ITEMS
            WHERE product_id = :productId
            """, nativeQuery = true)
    Long getTotalQuantitySold(@Param("productId") Integer productId);

    // 5. Calculate invoice total using MySQL stored function
    @Query(value = "SELECT calculate_invoice_total(:saleId)",
           nativeQuery = true)
    Double calculateInvoiceTotalFromFunction(
            @Param("saleId") Integer saleId);
}