package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.request.UpdateProfileRequest;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class UserResource {
  private final UserService userService;

  @Autowired
  UserResource(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/customer/me")
  public ResponseEntity<CustomerResponse> getMyProfile() {
    return userService.getCurrentCustomer();
  }

  @PutMapping("/customer/me")
  public ResponseEntity<CustomerResponse> updateMyProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(request);
  }

  @DeleteMapping("/customer/me")
  public ResponseEntity<Void> deleteMyAccount() {
    return userService.deleteSelf();
  }

  @GetMapping("/customer/{id}")
  public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
    return userService.getCustomer(id);
  }

  @GetMapping("/customers")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PageResponse<CustomerResponse>> getCustomers(
      @RequestParam(required = false) User.Status status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "desc") String sortDirection) {
    return userService.getAllCustomers(status, q, page, size, sortDirection);
  }

  @DeleteMapping("/admin/customer/{id}")
  public ResponseEntity<Void> disableCustomer(@PathVariable String id) {
    userService.updateUserStatus(id, User.Status.INACTIVE);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/admin/customer/{id}/block")
  public ResponseEntity<CustomerResponse> blockCustomer(@PathVariable String id) {
    return userService.updateUserStatus(id, User.Status.BLOCKED);
  }

  @PostMapping("/admin/customer/{id}/restore")
  public ResponseEntity<CustomerResponse> enableCustomer(@PathVariable String id) {
    return userService.updateUserStatus(id, User.Status.ACTIVE);
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
