package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
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
  private Roles role;

  public static CustomerResponse from(User user) {
    return new CustomerResponse(
        user.getId(), user.getFirstName(), user.getLastName(), user.getRole());
  }
}
