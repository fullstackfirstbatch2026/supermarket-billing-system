package com.example.supermarketbilling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.supermarketbilling.entity.Sale;

public interface SaleRepository extends JpaRepository<Sale, Integer> {

    // Get all sales
    @Query(value = "SELECT * FROM SALES ORDER BY sale_date DESC",
            nativeQuery = true)
    List<Sale> getAllSales();

    // Get sale by ID
    @Query(value = "SELECT * FROM SALES WHERE sale_id = :id",
            nativeQuery = true)
    Sale getSaleById(@Param("id") Integer id);

    // Get sales belonging to a customer
    @Query(value = "SELECT * FROM SALES WHERE customer_id = :customerId ORDER BY sale_date DESC",
            nativeQuery = true)
    List<Sale> getSalesByCustomerId(
            @Param("customerId") Integer customerId);

    // JOIN: Display sales with customer and product information
    @Query(value = """
            SELECT
                s.sale_id AS saleId,
                s.sale_date AS saleDate,
                c.customer_id AS customerId,
                c.customer_name AS customerName,
                p.product_id AS productId,
                p.product_name AS productName,
                si.quantity AS quantity,
                si.unit_price AS unitPrice,
                (si.quantity * si.unit_price) AS itemTotal
            FROM SALES s
            JOIN CUSTOMERS c ON s.customer_id = c.customer_id
            JOIN SALE_ITEMS si ON s.sale_id = si.sale_id
            JOIN PRODUCTS p ON si.product_id = p.product_id
            ORDER BY s.sale_date DESC
            """, nativeQuery = true)
    List<Object[]> getSalesWithCustomerAndProduct();
    @Query(
    	    value = "CALL sp_create_sale(:customerId, :productId, :quantity)",
    	    nativeQuery = true
    	)
    	void createSaleUsingProcedure(
    	    @Param("customerId") Integer customerId,
    	    @Param("productId") Integer productId,
    	    @Param("quantity") Integer quantity
    	);
}