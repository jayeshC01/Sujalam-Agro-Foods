package com.gryffindor.excalibur.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtils {

  private final static String secretKey = "8d4fed75477d160c393db8a22edce23a5ae7971b4533077d89ac0016dd92c879d21791073310294924cb896443a8214cfdc129baa42af8b3030a397382a93532";

  private final static long expiration = 123456789;

  private JwtUtils() {}

  private static SecretKey getSecretKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public static String generateToken(Authentication authentication) {
    return Jwts.builder()
        .subject(authentication.getName())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSecretKey())
        .compact();
  }

  private static Claims getClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSecretKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public static String getUsernameFromJWT(String token) {
    Claims claims = getClaims(token);
    return claims.getSubject();
  }

  public static boolean validateToken(String token, Authentication authentication) {
    try {
      Claims claims = getClaims(token);
      return !claims.getExpiration().before(new Date()) && claims.getSubject().equals(authentication.getName());
    }
    catch (Exception ex) {
      throw new AuthenticationCredentialsNotFoundException("JWT token is not valid " + token);
    }
  }

  public static boolean validateToken(String token) {
    try {
      Claims claims = getClaims(token);
      return !claims.getExpiration().before(new Date());
    } catch (Exception ex) {
      throw new AuthenticationCredentialsNotFoundException("JWT token is not valid " + token);
    }
  }
}
