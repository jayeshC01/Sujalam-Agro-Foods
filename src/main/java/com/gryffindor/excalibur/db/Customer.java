package com.gryffindor.excalibur.db;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customers")
public class Customer {
  public enum Roles {
    ADMIN,
    USER
  }

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "first_name", nullable = false)
  @NotBlank(message ="First name cannot be null")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "date_of_birth", nullable = false)
  private Date dateOfBirth;

  @Column(name = "username", unique = true, nullable = false)
  @NotBlank(message = "user name cannot be empty")
  private String userName;

  @Column(name= "password", nullable = false)
  @NotBlank(message = "password cannot be empty")
  private String password;

  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  private Roles role;
}
