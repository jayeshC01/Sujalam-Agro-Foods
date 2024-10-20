package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.repository.CustomerRepository;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomerService {
  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public ResponseEntity<String> addCustomer(Customer customer) {
    try {
      Customer existingCustomerByUserName = customerRepository.findByUserName(customer.getUserName()).orElse(null);
      if (existingCustomerByUserName != null) {
        return new ResponseEntity<>("Customer already exists", HttpStatus.BAD_REQUEST);
      }
      customer.setPassword(passwordEncoder.encode(customer.getPassword()));
      customerRepository.save(customer);
      return new ResponseEntity<>("Registered Successfully", HttpStatus.OK);
    } catch (ConstraintViolationException e) {
      throw new ConstraintViolationException(e.getConstraintViolations());
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Customer> getCustomer(final String id) {
    try {
      Customer customer = customerRepository.findById(id).orElse(null);
       if(customer == null) {
         return ResponseEntity.notFound().build();
       }
       return ResponseEntity.ok(customer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ResponseEntity<List<Customer>> getAllCustomers() {
    try {
      List<Customer> customers = customerRepository.findAll();
      if (customers.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(customers);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
