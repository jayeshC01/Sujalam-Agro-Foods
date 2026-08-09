package com.gryffindor.excalibur.config;

import java.security.Principal;

/**
 * Verified identity extracted from a Firebase ID token. {@link #getName()} returns the Firebase uid
 * so it doubles as the Spring Security {@code Authentication} name.
 */
public record FirebasePrincipal(String uid, String email, boolean emailVerified)
    implements Principal {

  @Override
  public String getName() {
    return uid;
  }
}
