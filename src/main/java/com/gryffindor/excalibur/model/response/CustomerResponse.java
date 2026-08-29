package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.db.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {
  private String id;
  private String email;
  private String firstName;
  private String lastName;
  private String phoneNumber;
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
