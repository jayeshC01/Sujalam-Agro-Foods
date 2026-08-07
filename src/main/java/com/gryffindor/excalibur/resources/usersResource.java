package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.CustomerResponse;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.services.usersService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class usersResource {
  private final usersService usersService;

  @Autowired
  usersResource(usersService usersService) {
    this.usersService = usersService;
  }

  @GetMapping("/customer/{id}")
  public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
    return usersService.getCustomer(id);
  }

  @GetMapping("/customers")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<CustomerResponse>> getCustomers() {
    return usersService.getAllCustomers();
  }

  @PostMapping("/customer/register")
  public ResponseEntity<String> registerCustomer(@RequestBody RegisterUser user) {
    return usersService.addUser(user, Roles.USER);
  }

  @PostMapping("/admin/register")
  public ResponseEntity<String> registerAdmin(@RequestBody RegisterUser user) {
    return usersService.addUser(user, Roles.ADMIN);
  }
}
