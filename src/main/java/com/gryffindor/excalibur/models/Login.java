package com.gryffindor.excalibur.models;

import lombok.Data;

@Data
public class Login {
  private String username;
  private String password;

  Login () {}

  Login (String username, String password) {
    this.username = username;
    this.password = password;
  }
}
