package com.example.supermarketbilling.controller;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.supermarketbilling.dto.CreateSaleRequest;
import com.example.supermarketbilling.dto.CreateMultiProductSaleRequest;
import com.example.supermarketbilling.entity.Sale;
import com.example.supermarketbilling.service.SaleService;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    // Get all sales
    @GetMapping
    public List<Sale> getAllSales() {
        return saleService.getAllSales();
    }

    // Get sale by ID
    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSaleById(
            @PathVariable Integer id) {

        Sale sale = saleService.getSaleById(id);

        if (sale == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sale);
    }

    // Get sales by customer ID
    @GetMapping("/customer/{customerId}")
    public List<Sale> getSalesByCustomer(
            @PathVariable Integer customerId) {

        return saleService.getSalesByCustomerId(customerId);
    }

    // Get sales with customer and product information using JOIN
    @GetMapping("/details")
    public List<Object[]> getSalesWithCustomerAndProduct() {
        return saleService.getSalesWithCustomerAndProduct();
    }

    // Create a sale directly
    @PostMapping
    public Sale createSale(@RequestBody Sale sale) {
        sale.setSaleId(null);
        return saleService.saveSale(sale);
    }

    // Existing single-product sale using stored procedure
    @PostMapping("/create")
    public ResponseEntity<?> createSaleUsingProcedure(
            @RequestBody CreateSaleRequest request) {

        if (request == null
                || request.getCustomerId() == null
                || request.getProductId() == null
                || request.getQuantity() == null
                || request.getCustomerId() <= 0
                || request.getProductId() <= 0
                || request.getQuantity() <= 0) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            "Customer ID, product ID and quantity must be positive values."
                    )
            );
        }

        try {
            Integer saleId = saleService.createSaleUsingProcedure(
                    request.getCustomerId(),
                    request.getProductId(),
                    request.getQuantity()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Sale created successfully",
                            "saleId", saleId
                    )
            );

        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            "Sale creation failed. Check customer ID, product ID and available stock."
                    )
            );
        }
    }

    // Create ONE sale with MULTIPLE products
    @PostMapping("/create-multiple")
    public ResponseEntity<?> createMultiProductSale(
            @RequestBody CreateMultiProductSaleRequest request) {

        if (request == null
                || request.getCustomerId() == null
                || request.getCustomerId() <= 0
                || request.getItems() == null
                || request.getItems().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            "Select a valid customer and add at least one product."
                    )
            );
        }

        try {
            Integer saleId =
                    saleService.createMultiProductSale(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message",
                            "Invoice created successfully",
                            "saleId",
                            saleId
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );

        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "error",
                            "Sale creation failed. Check customer, products and available stock."
                    )
            );
        }
    }

    // Delete sale
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(
            @PathVariable Integer id) {

        if (saleService.getSaleById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        saleService.deleteSale(id);

        return ResponseEntity.noContent().build();
    }
    
}