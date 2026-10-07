package com.example.supermarketbilling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.supermarketbilling.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    // Get all customers
    @Query(value = "SELECT * FROM CUSTOMERS", nativeQuery = true)
    List<Customer> getAllCustomers();

    // Get customer by ID
    @Query(value = "SELECT * FROM CUSTOMERS WHERE customer_id = :id",
            nativeQuery = true)
    Customer getCustomerById(@Param("id") Integer id);

    // Search customers by name
    @Query(value = "SELECT * FROM CUSTOMERS WHERE customer_name LIKE CONCAT('%', :name, '%')",
            nativeQuery = true)
    List<Customer> searchCustomersByName(@Param("name") String name);

    // Find customer by phone
    @Query(value = "SELECT * FROM CUSTOMERS WHERE phone = :phone",
            nativeQuery = true)
    Customer getCustomerByPhone(@Param("phone") String phone);

    // Find customer by email
    @Query(value = "SELECT * FROM CUSTOMERS WHERE email = :email",
            nativeQuery = true)
    Customer getCustomerByEmail(@Param("email") String email);
}