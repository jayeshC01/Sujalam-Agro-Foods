package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.config.FirebasePrincipal;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
  private final Validator validator;

  @Autowired
  UserService(
      UserRepository userRepository,
      MemberIdentityHandlerService memberIdentityHandlerService,
      Validator validator) {
    this.userRepository = userRepository;
    this.memberIdentityHandlerService = memberIdentityHandlerService;
    this.validator = validator;
  }

  @Transactional
  public ResponseEntity<String> addUser(RegisterUser request, Roles role) {
    FirebasePrincipal principal = memberIdentityHandlerService.getCurrentFirebasePrincipal();

    User existingUser = userRepository.findByFirebaseUid(principal.uid()).orElse(null);
    if (existingUser != null) {
      log.info("Registration attempted for already-registered user {}", existingUser.getId());
      return new ResponseEntity<>("User is already registered", HttpStatus.BAD_REQUEST);
    }

    User user = new User();
    user.setFirebaseUid(principal.uid());
    user.setEmail(principal.email());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setPhoneNumber(request.getPhoneNumber());
    user.setRole(role);

    Set<ConstraintViolation<User>> violations = validator.validate(user);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }

    userRepository.save(user);
    log.info("User {} registered with role {}", user.getId(), role);
    return new ResponseEntity<>("Registered Successfully", HttpStatus.OK);
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
  public ResponseEntity<PageResponse<CustomerResponse>> getAllCustomers(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    Page<User> users = userRepository.findAll(pageRequest);

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
}
