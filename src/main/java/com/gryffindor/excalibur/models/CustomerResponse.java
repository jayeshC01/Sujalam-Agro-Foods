package com.gryffindor.excalibur.models;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.db.User;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {
  private String id;
  private String firstName;
  private String lastName;
  private LocalDate dateOfBirth;
  private Roles role;

  public static CustomerResponse from(User user) {
    return new CustomerResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getDateOfBirth(),
        user.getRole());
  }
}
