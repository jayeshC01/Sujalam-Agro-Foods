package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class MemberIdentityHandlerService {
  private final UserRepository userRepository;

  @Autowired
  public MemberIdentityHandlerService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public String getLoggedInMemberID() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    Optional<User> user = userRepository.findByUserName(username);
    return user.map(User::getId).orElseThrow();

  }
}
