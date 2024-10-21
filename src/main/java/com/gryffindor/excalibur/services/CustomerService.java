package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.repository.CustomerRepository;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomerService {
  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
    this.customerRepository = customerRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public ResponseEntity<String> addUser(RegisterUser request, Roles role) {
    try {
      Customer existingCustomerByUserName = customerRepository.findByUserName(request.getUsername()).orElse(null);
      if (existingCustomerByUserName != null) {
        return new ResponseEntity<>("Customer already exists", HttpStatus.BAD_REQUEST);
      }
      Customer customer = new Customer();
      customer.setUserName(request.getUsername());
      customer.setFirstName(request.getFirstName());
      customer.setLastName(request.getLastName());
      customer.setRole(role);
      customer.setDateOfBirth(request.getDateOfBirth());
      customer.setPassword(passwordEncoder.encode(request.getPassword()));
      customerRepository.save(customer);
      return new ResponseEntity<>("Registered Successfully", HttpStatus.OK);
    } catch (ConstraintViolationException e) {
      throw new ConstraintViolationException(e.getConstraintViolations());
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

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
