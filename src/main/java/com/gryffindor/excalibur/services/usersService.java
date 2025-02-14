package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class usersService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  usersService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public ResponseEntity<String> addUser(RegisterUser request, Roles role) {
    try {
      User existingUserByUserName = userRepository.findByUserName(request.getUsername()).orElse(null);
      if (existingUserByUserName != null) {
        return new ResponseEntity<>("Customer already exists", HttpStatus.BAD_REQUEST);
      }

      User user = new User();
      user.setUserName(request.getUsername());
      user.setFirstName(request.getFirstName());
      user.setLastName(request.getLastName());
      user.setRole(role);
      user.setDateOfBirth(request.getDateOfBirth());
      user.setPassword(passwordEncoder.encode(request.getPassword()));
      userRepository.save(user);
      return new ResponseEntity<>("Registered Successfully", HttpStatus.OK);
    } catch (ConstraintViolationException e) {
      throw new ConstraintViolationException("Data Constraint Failed. Please provide details",e.getConstraintViolations());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ResponseEntity<User> getCustomer(final String id) {
      User user = userRepository.findById(id)
              .orElseThrow(() -> new EntityNotFoundException("Customer with id "+id+" not found"));
      return ResponseEntity.ok(user);
  }

  public ResponseEntity<List<User>> getAllCustomers() {
      List<User> users = userRepository.findAll();
      if (users.isEmpty()) {
        throw new EntityNotFoundException("No Customers were found");
      }
      return ResponseEntity.ok(users);
  }
}
