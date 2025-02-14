package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.SimpleResponse;
import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
public class usersService {
  private final UserRepository userRepository;
  private final Validator validator;

  @Autowired
  usersService(UserRepository userRepository, Validator validator) {
    this.userRepository = userRepository;
    this.validator = validator;
  }

  @Transactional
  public SimpleResponse<String> addUser(RegisterUser request, Roles role) {
      User existingUserByUserName = userRepository.findByUserName(request.getUsername()).orElse(null);
      if (existingUserByUserName != null) {
        return SimpleResponse.<String>builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("User already exists")
                .build();
      }

      User user = new User();
      user.setUserName(request.getUsername());
      user.setFirstName(request.getFirstName());
      user.setLastName(request.getLastName());
      user.setRole(role);
      user.setDateOfBirth(request.getDateOfBirth());
      user.setPassword(request.getPassword());

      Set<ConstraintViolation<User>> violations = validator.validate(user);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }

      userRepository.save(user);
      return SimpleResponse.<String>builder()
              .status(HttpStatus.OK)
              .message("User Added successfully")
              .build();
  }

  public SimpleResponse<User> getCustomer(final String id) {
      User user = userRepository.findById(id)
              .orElseThrow(() -> new EntityNotFoundException("Customer with id "+id+" not found"));
      return SimpleResponse.<User>builder()
              .status(HttpStatus.OK)
              .message("Success")
              .content(user)
              .build();
  }

  public SimpleResponse<List<User>> getAllCustomers() {
      List<User> users = userRepository.findAll();
      if (users.isEmpty()) {
        throw new EntityNotFoundException("No Customers were found");
      }
      return SimpleResponse.<List<User>>builder()
              .status(HttpStatus.OK)
              .message("Success")
              .content(users)
              .build();
  }
}
