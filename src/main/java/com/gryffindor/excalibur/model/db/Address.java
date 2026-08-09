package com.gryffindor.excalibur.model.db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Embeddable
@Data
public class Address {

  @Column(name = "recipient_name", nullable = false)
  @NotBlank(message = "Recipient name cannot be empty")
  private String recipientName;

  @Column(name = "phone_number", nullable = false)
  @NotBlank(message = "Recipient phone number cannot be empty")
  private String phoneNumber;

  @Column(name = "address_line1", nullable = false)
  @NotBlank(message = "Address line 1 cannot be empty")
  private String addressLine1;

  @Column(name = "address_line2")
  private String addressLine2;

  @Column(name = "city", nullable = false)
  @NotBlank(message = "City cannot be empty")
  private String city;

  @Column(name = "state", nullable = false)
  @NotBlank(message = "State cannot be empty")
  private String state;

  @Column(name = "postal_code", nullable = false)
  @NotBlank(message = "Postal code cannot be empty")
  private String postalCode;

  @Column(name = "country", nullable = false)
  @NotBlank(message = "Country cannot be empty")
  private String country;
}
