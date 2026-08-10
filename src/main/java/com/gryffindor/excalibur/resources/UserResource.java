package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.services.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserResource {
  private final UserService userService;

  @Autowired
  UserResource(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/customer/{id}")
  public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
    return userService.getCustomer(id);
  }

  @GetMapping("/customers")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<CustomerResponse>> getCustomers() {
    return userService.getAllCustomers();
  }

  @PostMapping("/customer/register")
  public ResponseEntity<String> registerCustomer(@Valid @RequestBody RegisterUser user) {
    return userService.addUser(user, Roles.USER);
  }

  @PostMapping("/admin/register")
  public ResponseEntity<String> registerAdmin(@Valid @RequestBody RegisterUser user) {
    return userService.addUser(user, Roles.ADMIN);
  }
}
