package com.example.supermarketbilling.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.supermarketbilling.entity.Customer;
import com.example.supermarketbilling.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.getAllCustomers();
    }

    public Customer getCustomerById(Integer id) {
        return customerRepository.getCustomerById(id);
    }

    public List<Customer> searchCustomersByName(String name) {
        return customerRepository.searchCustomersByName(name);
    }

    public Customer getCustomerByPhone(String phone) {
        return customerRepository.getCustomerByPhone(phone);
    }

    public Customer getCustomerByEmail(String email) {
        return customerRepository.getCustomerByEmail(email);
    }

    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }
}