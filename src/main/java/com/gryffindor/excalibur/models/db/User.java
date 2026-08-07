package com.gryffindor.excalibur.models.db;

import com.gryffindor.excalibur.constants.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.*;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "users")
public class User extends AuditStamp {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "firebase_uid", nullable = false, unique = true, updatable = false)
  @NotBlank(message = "Firebase UID cannot be empty")
  private String firebaseUid;

  @Column(name = "email", nullable = false, unique = true)
  @NotBlank(message = "Email cannot be empty")
  @Email(message = "Email must be a valid email address")
  private String email;

  @Column(name = "first_name", nullable = false)
  @NotBlank(message = "First name cannot be empty")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "phone_number", unique = true)
  private String phoneNumber;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private Roles role = Roles.USER;

  @Column(name = "active", nullable = false)
  private boolean active = true;
}
