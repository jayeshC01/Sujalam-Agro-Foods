package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.models.AuthenticationRequest;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.services.CustomerService;
import com.gryffindor.excalibur.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CustomerResource {
  private final CustomerService customerService;
  private final AuthenticationManager authenticationManager;

  @Autowired
  CustomerResource(CustomerService customerService, AuthenticationManager authenticationManager) {
    this.customerService = customerService;
    this.authenticationManager = authenticationManager;
  }

  @GetMapping("/customer/{id}")
  public ResponseEntity<Customer> getCustomer(@PathVariable String id) {
    return customerService.getCustomer(id);
  }

  @GetMapping("/customers")
  public ResponseEntity<List<Customer>> getCustomers() {
    return customerService.getAllCustomers();
  }

  @PostMapping("/customer/register")
  public ResponseEntity<String> registerCustomer(@RequestBody RegisterUser user) {
    return customerService.addUser(user, Roles.USER);
  }

  @PostMapping("/admin/register")
  public ResponseEntity<String> registerAdmin(@RequestBody RegisterUser user) {
    return customerService.addUser(user, Roles.ADMIN);
  }

  @PostMapping("/authenticate")
  public ResponseEntity<String> authenticateCustomer(@RequestBody AuthenticationRequest authenticationDetails) {
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
