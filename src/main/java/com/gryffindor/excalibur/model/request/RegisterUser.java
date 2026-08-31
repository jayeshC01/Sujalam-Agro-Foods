package com.gryffindor.excalibur.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUser {
  @NotBlank(message = "First name cannot be empty")
  private String firstName;

  private String lastName;

  @NotBlank(message = "Phone number cannot be empty")
  @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be a valid 10-digit number")
  private String phoneNumber;
}
