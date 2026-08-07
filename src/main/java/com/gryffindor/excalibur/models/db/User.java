package com.gryffindor.excalibur.models.db;

import com.gryffindor.excalibur.constants.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.*;

@Entity
@Data
@Table(name = "users")
public class User {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "firebase_uid", unique = true, nullable = false)
  @NotBlank(message = "Firebase uid cannot be empty")
  private String firebaseUid;

  @Column(name = "email", unique = true, nullable = false)
  @NotBlank(message = "Email cannot be empty")
  private String email;

  @Column(name = "first_name", nullable = false)
  @NotBlank(message = "First name cannot be null")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "date_of_birth", nullable = false)
  private LocalDate dateOfBirth;

  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  private Roles role;
}
