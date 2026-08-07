package com.gryffindor.excalibur.models.db;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Embeddable
@Data
public class Address {
  @NotBlank(message = "Recipient name cannot be empty")
  private String recipientName;

  @NotBlank(message = "Phone number cannot be empty")
  private String phoneNumber;

  @NotBlank(message = "Address line 1 cannot be empty")
  private String addressLine1;

  @NotBlank(message = "City cannot be empty")
  private String city;

  @NotBlank(message = "State cannot be empty")
  private String state;

  @NotBlank(message = "Postal code cannot be empty")
  private String postalCode;

  @NotBlank(message = "Country cannot be empty")
  private String country;
}
