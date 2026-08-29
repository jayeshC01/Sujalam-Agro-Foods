package com.gryffindor.excalibur.model.db;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Embeddable
@Data
@Schema(description = "Shipping delivery address")
public class Address {

  @Column(name = "recipient_name", nullable = false)
  @NotBlank(message = "Recipient name cannot be empty")
  @Schema(
      description = "Full name of the recipient",
      example = "John Doe",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String recipientName;

  @Column(name = "phone_number", nullable = false)
  @NotBlank(message = "Recipient phone number cannot be empty")
  @Schema(
      description = "Contact phone number for delivery",
      example = "9876543210",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String phoneNumber;

  @Column(name = "address_line1", nullable = false)
  @NotBlank(message = "Address line 1 cannot be empty")
  @Schema(
      description = "Street address line 1",
      example = "Flat 402, Green Meadows",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String addressLine1;

  @Column(name = "address_line2")
  @Schema(description = "Street address line 2 / Landmark", example = "Near City Park")
  private String addressLine2;

  @Column(name = "city", nullable = false)
  @NotBlank(message = "City cannot be empty")
  @Schema(description = "City name", example = "Pune", requiredMode = Schema.RequiredMode.REQUIRED)
  private String city;

  @Column(name = "state", nullable = false)
  @NotBlank(message = "State cannot be empty")
  @Schema(
      description = "State name",
      example = "Maharashtra",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String state;

  @Column(name = "postal_code", nullable = false)
  @NotBlank(message = "Postal code cannot be empty")
  @Schema(
      description = "Postal PIN code",
      example = "411001",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String postalCode;

  @Column(name = "country", nullable = false)
  @NotBlank(message = "Country cannot be empty")
  @Schema(description = "Country", example = "India", requiredMode = Schema.RequiredMode.REQUIRED)
  private String country;
}
