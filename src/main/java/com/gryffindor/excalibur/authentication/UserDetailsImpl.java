//package com.gryffindor.excalibur.authentication;
//
//import com.gryffindor.excalibur.models.db.User;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//
//public class UserDetailsImpl implements UserDetails {
//  private final String username;
//  private final String password;
//  private final List<GrantedAuthority> authorities;
//
//
//  UserDetailsImpl(User user) {
//    username = user.getUserName();
//    password = user.getPassword();
//    authorities = Collections.singletonList(
//        new SimpleGrantedAuthority(user.getRole().toString()));
//
//  }
//
//  @Override
//  public Collection<? extends GrantedAuthority> getAuthorities() {
//    return authorities;
//  }
//
//  @Override
//  public String getPassword() {
//    return password;
//  }
//
//  @Override
//  public String getUsername() {
//    return username;
//  }
//
//  @Override
//  public boolean isAccountNonExpired() {
//    return UserDetails.super.isAccountNonExpired();
//  }
//
//  @Override
//  public boolean isAccountNonLocked() {
//    return UserDetails.super.isAccountNonLocked();
//  }
//
//  @Override
//  public boolean isCredentialsNonExpired() {
//    return UserDetails.super.isCredentialsNonExpired();
//  }
//
//  @Override
//  public boolean isEnabled() {
//    return UserDetails.super.isEnabled();
//  }
//}
