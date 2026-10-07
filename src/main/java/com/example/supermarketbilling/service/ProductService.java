package com.example.supermarketbilling.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.supermarketbilling.entity.Product;
import com.example.supermarketbilling.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.getAllProducts();
    }

    public Product getProductById(Integer id) {
        return productRepository.getProductById(id);
    }

    public List<Product> searchProductsByName(String name) {
        return productRepository.searchProductsByName(name);
    }

    public List<Product> getAvailableProducts() {
        return productRepository.getAvailableProducts();
    }

    public List<Product> getProductsSoldAboveAverage() {
        return productRepository.findProductsSoldAboveAverage();
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public void deleteProduct(Integer id) {
        productRepository.deleteById(id);
    }
}