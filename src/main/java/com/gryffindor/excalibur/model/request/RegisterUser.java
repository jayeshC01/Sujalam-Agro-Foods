package com.gryffindor.excalibur.model.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Identity fields (firebaseUid, email, emailVerified) are never taken from the request body -
// they come from the verified Firebase ID token, otherwise a caller could register as anyone.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUser {
  @NotBlank(message = "First name cannot be empty")
  private String firstName;

  private String lastName;
  private String phoneNumber;
  private LocalDate dob;
}
