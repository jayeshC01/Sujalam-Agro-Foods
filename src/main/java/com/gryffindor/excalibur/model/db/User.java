package com.gryffindor.excalibur.model.db;

import com.gryffindor.excalibur.model.common.AuditStamp;
import com.gryffindor.excalibur.model.constants.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(
    name = "users",
    indexes = {
      @Index(name = "idx_users_email_status", columnList = "email, status"),
      @Index(name = "idx_users_firebase_uid", columnList = "firebase_uid")
    })
public class User extends AuditStamp {

  public enum Status {
    ACTIVE,
    INACTIVE,
    BLOCKED
  }

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "firebase_uid", nullable = false, unique = true, updatable = false)
  @NotBlank(message = "Firebase UID cannot be empty")
  private String firebaseUid;

  @Column(name = "email", nullable = false)
  @NotBlank(message = "Email cannot be empty")
  @Email(message = "Email must be a valid email address")
  private String email;

  @Column(name = "first_name", nullable = false)
  @NotBlank(message = "First name cannot be empty")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "phone_number", nullable = false)
  @NotBlank(message = "Phone number cannot be empty")
  private String phoneNumber;

  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  private Roles role = Roles.USER;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private Status status = Status.ACTIVE;

  public boolean isActive() {
    return this.status == Status.ACTIVE;
  }

  public boolean isBlocked() {
    return this.status == Status.BLOCKED;
  }
}
