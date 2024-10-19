package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class CustomerResource {
  @Autowired
  private CustomerService customerService;

  @GetMapping("/admin/customer/{id}")
  public ResponseEntity<Customer> getCustomer(@PathVariable String id) {
    return customerService.getCustomer(id);
  }

  @GetMapping("/admin/customers")
  public ResponseEntity<List<Customer>> getCustomers() {
    return customerService.getAllCustomers();
  }

  @PostMapping("/register")
  public ResponseEntity<String> registerCustomer(@RequestBody Customer customer) {
    return customerService.addCustomer(customer);
  }
}
