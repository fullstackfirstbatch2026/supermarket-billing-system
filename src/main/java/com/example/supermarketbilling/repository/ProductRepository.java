package com.example.supermarketbilling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.supermarketbilling.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // 1. Get all products
    @Query(value = "SELECT * FROM PRODUCTS", nativeQuery = true)
    List<Product> getAllProducts();

    // 2. Get product by ID
    @Query(value = "SELECT * FROM PRODUCTS WHERE product_id = :id",
           nativeQuery = true)
    Product getProductById(@Param("id") Integer id);

    // 3. Search products by name
    @Query(value = "SELECT * FROM PRODUCTS WHERE product_name LIKE CONCAT('%', :name, '%')",
           nativeQuery = true)
    List<Product> searchProductsByName(@Param("name") String name);

    // 4. Get available products
    @Query(value = "SELECT * FROM PRODUCTS WHERE quantity > 0",
           nativeQuery = true)
    List<Product> getAvailableProducts();

    // 5. Find products sold above average quantity using subquery
    @Query(value = """
            SELECT p.*
            FROM PRODUCTS p
            JOIN SALE_ITEMS si ON p.product_id = si.product_id
            GROUP BY p.product_id, p.product_name, p.price,
                     p.quantity, p.category
            HAVING SUM(si.quantity) > (
                SELECT AVG(product_quantity)
                FROM (
                    SELECT SUM(quantity) AS product_quantity
                    FROM SALE_ITEMS
                    GROUP BY product_id
                ) AS average_sales
            )
            """, nativeQuery = true)
    List<Product> findProductsSoldAboveAverage();
}