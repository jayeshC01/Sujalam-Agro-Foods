package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.request.UpdateProfileRequest;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "Users & Customers",
    description =
        "Customer profile management, account self-service, and admin customer administration")
public class UserResource {
  private final UserService userService;

  @Autowired
  UserResource(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/customer/me")
  @Operation(
      summary = "Get current customer profile",
      description = "Retrieves profile details of the authenticated logged-in customer")
  public ResponseEntity<CustomerResponse> getMyProfile() {
    return userService.getCurrentCustomer();
  }

  @PutMapping("/customer/me")
  @Operation(
      summary = "Update current customer profile",
      description = "Updates name and phone number for the authenticated customer")
  public ResponseEntity<CustomerResponse> updateMyProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(request);
  }

  @DeleteMapping("/customer/me")
  @Operation(
      summary = "Self-deactivate account",
      description = "Deactivates the customer's account and revokes Firebase access")
  public ResponseEntity<Void> deleteMyAccount() {
    return userService.deleteSelf();
  }

  @GetMapping("/customer/{id}")
  @Operation(
      summary = "Get customer by ID",
      description = "Retrieves customer profile by ID (Owner or Admin)")
  public ResponseEntity<CustomerResponse> getCustomer(
      @Parameter(description = "Customer user unique identifier", example = "usr_456") @PathVariable
          String id) {
    return userService.getCustomer(id);
  }

  @GetMapping("/customers")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Admin: List all customers",
      description =
          "Paginated customer list for administrators with optional status and search filters")
  public ResponseEntity<PageResponse<CustomerResponse>> getCustomers(
      @Parameter(description = "Filter by customer status (ACTIVE, INACTIVE, BLOCKED)")
          @RequestParam(required = false)
          User.Status status,
      @Parameter(
              description = "Search query matching customer name, email, or phone",
              example = "john")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Page number (0-based index)", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Number of items per page (1-100)", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(1)
          @Max(100)
          int size,
      @Parameter(description = "Sort direction by registration date (asc, desc)", example = "desc")
          @RequestParam(defaultValue = "desc")
          String sortDirection) {
    return userService.getAllCustomers(status, q, page, size, sortDirection);
  }

  @DeleteMapping("/admin/customer/{id}")
  @Operation(
      summary = "Admin: Deactivate customer",
      description = "Deactivates a customer account and deletes their Firebase login")
  public ResponseEntity<Void> disableCustomer(
      @Parameter(description = "Customer user unique identifier", example = "usr_456") @PathVariable
          String id) {
    userService.updateUserStatus(id, User.Status.INACTIVE);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/admin/customer/{id}/block")
  @Operation(
      summary = "Admin: Block customer",
      description = "Blocks a customer account and disables their Firebase login")
  public ResponseEntity<CustomerResponse> blockCustomer(
      @Parameter(description = "Customer user unique identifier", example = "usr_456") @PathVariable
          String id) {
    return userService.updateUserStatus(id, User.Status.BLOCKED);
  }

  @PostMapping("/admin/customer/{id}/restore")
  @Operation(
      summary = "Admin: Restore customer",
      description =
          "Restores a customer account back to ACTIVE and re-enables their Firebase login")
  public ResponseEntity<CustomerResponse> enableCustomer(
      @Parameter(description = "Customer user unique identifier", example = "usr_456") @PathVariable
          String id) {
    return userService.updateUserStatus(id, User.Status.ACTIVE);
  }

  @PostMapping("/customer/register")
  @Operation(
      summary = "Register customer profile",
      description = "Completes customer profile creation after Firebase signup")
  public ResponseEntity<String> registerCustomer(@Valid @RequestBody RegisterUser user) {
    return userService.addUser(user, Roles.USER);
  }

  @PostMapping("/admin/register")
  @Operation(
      summary = "Admin: Register new admin",
      description = "Creates a new admin account (Admin only)")
  public ResponseEntity<String> registerAdmin(@Valid @RequestBody RegisterUser user) {
    return userService.addUser(user, Roles.ADMIN);
  }
}
