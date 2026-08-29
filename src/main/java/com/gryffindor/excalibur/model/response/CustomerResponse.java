package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.db.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Customer profile representation")
public class CustomerResponse {
  @Schema(
      description = "Customer user ID",
      example = "usr_1001",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Schema(
      description = "Customer verified email address",
      example = "customer@example.com",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String email;

  @Schema(
      description = "Customer first name",
      example = "John",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String firstName;

  @Schema(
      description = "Customer last name (Nullable/Optional)",
      example = "Doe",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String lastName;

  @Schema(
      description = "Customer contact phone number",
      example = "9876543210",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String phoneNumber;

  @Schema(
      description = "Account status (ACTIVE, INACTIVE, BLOCKED)",
      example = "ACTIVE",
      allowableValues = {"ACTIVE", "INACTIVE", "BLOCKED"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  private User.Status status;

  public static CustomerResponse from(User user) {
    return CustomerResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .phoneNumber(user.getPhoneNumber())
        .status(user.getStatus())
        .build();
  }
}
