package com.gryffindor.excalibur.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.gryffindor.excalibur.config.FirebasePrincipal;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.exception.AuthenticationProviderException;
import com.gryffindor.excalibur.model.exception.InvalidRequestException;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.request.UpdateProfileRequest;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final MemberIdentityHandlerService memberIdentityHandlerService;
  private final FirebaseAuth firebaseAuth;

  @Autowired
  UserService(
      UserRepository userRepository,
      MemberIdentityHandlerService memberIdentityHandlerService,
      FirebaseAuth firebaseAuth) {
    this.userRepository = userRepository;
    this.memberIdentityHandlerService = memberIdentityHandlerService;
    this.firebaseAuth = firebaseAuth;
  }

  @Transactional
  public ResponseEntity<String> addUser(RegisterUser request, Roles role) {
    FirebasePrincipal principal = memberIdentityHandlerService.getCurrentFirebasePrincipal();

    Optional<User> existingByUid = userRepository.findByFirebaseUid(principal.uid());
    if (existingByUid.isPresent()) {
      User user = existingByUid.get();
      if (user.isBlocked()) {
        log.warn("Blocked user {} attempted registration", user.getId());
        return new ResponseEntity<>(
            "Your account is blocked. Please contact support.", HttpStatus.FORBIDDEN);
      }
      if (user.isActive()) {
        log.info("Registration attempted for already-registered active user {}", user.getId());
        return new ResponseEntity<>("User is already registered", HttpStatus.BAD_REQUEST);
      }
    }

    Optional<User> existingActiveEmail =
        userRepository.findByEmailAndStatus(principal.email(), User.Status.ACTIVE);
    if (existingActiveEmail.isPresent()) {
      log.info(
          "Registration attempted with email {} which already has an active account",
          principal.email());
      return new ResponseEntity<>(
          "An active account with this email already exists", HttpStatus.BAD_REQUEST);
    }

    User user = new User();
    user.setFirebaseUid(principal.uid());
    user.setEmail(principal.email());
    user.setFirstName(request.getFirstName().trim());
    user.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
    user.setPhoneNumber(request.getPhoneNumber().trim());
    user.setRole(role);
    user.setStatus(User.Status.ACTIVE);

    userRepository.save(user);
    log.info("User {} registered with role {}", user.getId(), role);
    return new ResponseEntity<>("Registered Successfully", HttpStatus.OK);
  }

  @Transactional(readOnly = true)
  public ResponseEntity<CustomerResponse> getCurrentCustomer() {
    User currentUser = memberIdentityHandlerService.getLoggedInUser();
    return ResponseEntity.ok(CustomerResponse.from(currentUser));
  }

  @Transactional
  public ResponseEntity<CustomerResponse> updateProfile(UpdateProfileRequest request) {
    User currentUser = memberIdentityHandlerService.getLoggedInUser();
    currentUser.setFirstName(request.getFirstName().trim());
    currentUser.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
    currentUser.setPhoneNumber(request.getPhoneNumber().trim());

    userRepository.save(currentUser);
    log.info("User {} updated profile", currentUser.getId());
    return ResponseEntity.ok(CustomerResponse.from(currentUser));
  }

  @Transactional
  public ResponseEntity<Void> deleteSelf() {
    User currentUser = memberIdentityHandlerService.getLoggedInUser();
    if (currentUser.getRole() == Roles.ADMIN) {
      throw new AccessDeniedException("Admins cannot delete their own account");
    }
    if (currentUser.getStatus() != User.Status.INACTIVE) {
      currentUser.setStatus(User.Status.INACTIVE);
      userRepository.save(currentUser);
      deleteFirebaseUser(currentUser.getFirebaseUid());
      log.info("User {} self-deactivated account", currentUser.getId());
    }
    return ResponseEntity.noContent().build();
  }

  @Transactional
  public ResponseEntity<CustomerResponse> updateUserStatus(String id, User.Status status) {
    User currentUser = memberIdentityHandlerService.requireAdmin();
    if (currentUser.getId().equals(id) && status != User.Status.ACTIVE) {
      throw new AccessDeniedException("Admins cannot disable or block their own account");
    }

    User target =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User with id " + id + " not found"));

    target.setStatus(status);
    userRepository.save(target);

    switch (status) {
      case INACTIVE -> deleteFirebaseUser(target.getFirebaseUid());
      case BLOCKED -> setFirebaseUserDisabled(target.getFirebaseUid(), true);
      case ACTIVE -> setFirebaseUserDisabled(target.getFirebaseUid(), false);
    }

    log.info("Admin updated user {} status to {}", id, status);
    return ResponseEntity.ok(CustomerResponse.from(target));
  }

  private void deleteFirebaseUser(String firebaseUid) {
    try {
      firebaseAuth.deleteUser(firebaseUid);
      log.info("Deleted user {} from Firebase Auth", firebaseUid);
    } catch (FirebaseAuthException e) {
      if ("USER_NOT_FOUND".equalsIgnoreCase(String.valueOf(e.getAuthErrorCode()))
          || (e.getMessage() != null && e.getMessage().contains("user-not-found"))) {
        log.info("User {} already removed from Firebase Auth (no-op)", firebaseUid);
        return;
      }
      log.error("Failed to delete user {} from Firebase Auth: {}", firebaseUid, e.getMessage());
      throw new AuthenticationProviderException(
          "Failed to delete account from authentication provider. Please try again.", e);
    }
  }

  private void setFirebaseUserDisabled(String firebaseUid, boolean disabled) {
    try {
      UserRecord.UpdateRequest updateRequest =
          new UserRecord.UpdateRequest(firebaseUid).setDisabled(disabled);
      firebaseAuth.updateUser(updateRequest);
      log.info("Set user {} disabled={} in Firebase Auth", firebaseUid, disabled);
    } catch (FirebaseAuthException e) {
      log.error(
          "Failed to update user {} disabled status to {} in Firebase Auth: {}",
          firebaseUid,
          disabled,
          e.getMessage());
      throw new AuthenticationProviderException(
          "Failed to update user status in authentication provider. Please try again.", e);
    }
  }

  @Transactional(readOnly = true)
  public ResponseEntity<CustomerResponse> getCustomer(final String id) {
    User currentUser = memberIdentityHandlerService.getLoggedInUser();
    if (currentUser.getRole() != Roles.ADMIN && !currentUser.getId().equals(id)) {
      throw new AccessDeniedException("You are not allowed to view this customer");
    }

    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Customer with id " + id + " not found"));
    return ResponseEntity.ok(CustomerResponse.from(user));
  }

  @Transactional(readOnly = true)
  public ResponseEntity<PageResponse<CustomerResponse>> getAllCustomers(
      User.Status status, String query, int page, int size, String sortDirection) {
    PageRequest pageRequest = PageRequest.of(page, size, createSort(sortDirection));
    Page<User> users = userRepository.searchCustomers(status, query, pageRequest);

    List<CustomerResponse> content =
        users.getContent().stream().map(CustomerResponse::from).toList();
    PageResponse<CustomerResponse> pageResponse =
        PageResponse.<CustomerResponse>builder()
            .content(content)
            .page(users.getNumber())
            .size(users.getSize())
            .totalElements(users.getTotalElements())
            .totalPages(users.getTotalPages())
            .first(users.isFirst())
            .last(users.isLast())
            .build();

    return ResponseEntity.ok(pageResponse);
  }

  private Sort createSort(String sortDirection) {
    String trimmedDirection = sortDirection != null ? sortDirection.trim().toLowerCase() : "desc";
    Sort.Direction direction =
        switch (trimmedDirection) {
          case "asc" -> Sort.Direction.ASC;
          case "desc" -> Sort.Direction.DESC;
          default ->
              throw new InvalidRequestException(
                  "Invalid sort direction: '" + sortDirection + "'. Allowed values: asc, desc");
        };

    return Sort.by(direction, "createdAt");
  }
}
