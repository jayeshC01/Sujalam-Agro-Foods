package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.authentication.UserDetailServiceImpl;
import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.models.Login;
import com.gryffindor.excalibur.services.CustomerService;
import com.gryffindor.excalibur.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CustomerResource {
  @Autowired
  private CustomerService customerService;

  @Autowired
  AuthenticationManager authenticationManager;
  @Autowired
  private UserDetailServiceImpl userDetailService;

  @GetMapping("/admin/customer/{id}")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Customer> getCustomer(@PathVariable String id) {
    return customerService.getCustomer(id);
  }

  @GetMapping("/admin/customers")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<List<Customer>> getCustomers() {
    return customerService.getAllCustomers();
  }

  @PostMapping("/register")
  public ResponseEntity<String> registerCustomer(@RequestBody Customer customer) {
    return customerService.addCustomer(customer);
  }

  @PostMapping("/authenticate")
  public ResponseEntity<String> authenticateCustomer(@RequestBody Login authenticationDetails) {
    try {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(authenticationDetails.getUsername(), authenticationDetails.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String token = JwtUtils.generateToken(authentication);
    return ResponseEntity.ok(token);
  } catch (BadCredentialsException e) {
    return new ResponseEntity<>("Invalid password or error", HttpStatus.BAD_REQUEST);
    }
  }
}
