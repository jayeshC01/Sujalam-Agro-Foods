package com.gryffindor.excalibur.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Identity fields (firebaseUid, email, emailVerified) are never taken from the request body -
// they come from the verified Firebase ID token, otherwise a caller could register as anyone.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request body payload for registering a new user profile")
public class RegisterUser {
  @NotBlank(message = "First name cannot be empty")
  @Schema(
      description = "User first name",
      example = "John",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String firstName;

  @Schema(description = "User last name", example = "Doe")
  private String lastName;

  @NotBlank(message = "Phone number cannot be empty")
  @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be a valid 10-digit number")
  @Schema(
      description = "10-digit mobile phone number",
      example = "9876543210",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String phoneNumber;
}
