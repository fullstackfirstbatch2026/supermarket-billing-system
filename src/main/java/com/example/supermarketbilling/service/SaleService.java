package com.example.supermarketbilling.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.supermarketbilling.entity.Sale;
import com.example.supermarketbilling.repository.SaleRepository;
import com.example.supermarketbilling.dto.CreateMultiProductSaleRequest;
import com.example.supermarketbilling.dto.CreateMultiProductSaleRequest.SaleItemRequest;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final JdbcTemplate jdbcTemplate;

    public SaleService(
            SaleRepository saleRepository,
            JdbcTemplate jdbcTemplate) {
        this.saleRepository = saleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Sale> getAllSales() {
        return saleRepository.getAllSales();
    }

    public Sale getSaleById(Integer id) {
        return saleRepository.getSaleById(id);
    }

    public List<Sale> getSalesByCustomerId(Integer customerId) {
        return saleRepository.getSalesByCustomerId(customerId);
    }

    public List<Object[]> getSalesWithCustomerAndProduct() {
        return saleRepository.getSalesWithCustomerAndProduct();
    }

    @Transactional
    public Sale saveSale(Sale sale) {
        return saleRepository.save(sale);
    }

    @Transactional
    public void deleteSale(Integer id) {
        saleRepository.deleteById(id);
    }

    // Existing single-product stored procedure
    @Transactional
    public Integer createSaleUsingProcedure(
            Integer customerId,
            Integer productId,
            Integer quantity) {

        return jdbcTemplate.queryForObject(
                "CALL sp_create_sale(?, ?, ?)",
                Integer.class,
                customerId,
                productId,
                quantity
        );
    }	

    // Create ONE sale with MULTIPLE products
    @Transactional
    public Integer createMultiProductSale(
            CreateMultiProductSaleRequest request) {

        Integer customerId = request.getCustomerId();
        List<SaleItemRequest> items = request.getItems();

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Validate customer exists
        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE customer_id = ?",
                Integer.class,
                customerId
        );

        if (customerCount == null || customerCount == 0) {
            throw new IllegalArgumentException("Customer not found");
        }

        // Validate all products and quantities before creating the sale
        for (SaleItemRequest item : items) {

            if (item == null
                    || item.getProductId() == null
                    || item.getProductId() <= 0
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Every item must have a valid product ID and quantity"
                );
            }

            Integer stock = jdbcTemplate.queryForObject(
                    "SELECT quantity FROM products WHERE product_id = ? FOR UPDATE",
                    Integer.class,
                    item.getProductId()
            );

            if (stock == null) {
                throw new IllegalArgumentException(
                        "Product not found: " + item.getProductId()
                );
            }

            if (stock < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product ID: "
                                + item.getProductId()
                );
            }
        }

        // Create ONE bill header
        jdbcTemplate.update(
                "INSERT INTO sales (customer_id, sale_date) VALUES (?, CURRENT_TIMESTAMP)",
                customerId
        );

        Integer saleId = jdbcTemplate.queryForObject(
                "SELECT LAST_INSERT_ID()",
                Integer.class
        );

        if (saleId == null) {
            throw new IllegalStateException("Could not create sale");
        }

        // Add every product to the same bill
        for (SaleItemRequest item : items) {

            Double price = jdbcTemplate.queryForObject(
                    "SELECT price FROM products WHERE product_id = ?",
                    Double.class,
                    item.getProductId()
            );

            if (price == null) {
                throw new IllegalArgumentException(
                        "Price not found for product ID: "
                                + item.getProductId()
                );
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO sale_items
                        (sale_id, product_id, quantity, unit_price)
                    VALUES (?, ?, ?, ?)
                    """,
                    saleId,
                    item.getProductId(),
                    item.getQuantity(),
                    price
            );
            
        }

        return saleId;
    }
}