package com.gryffindor.excalibur.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUser {
  private String username;
  private String password;
  private String firstName;
  private String lastName;
  private Date dateOfBirth;
}
