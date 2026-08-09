package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.config.FirebasePrincipal;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.repository.UserRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MemberIdentityHandlerService {
  private final UserRepository userRepository;

  @Autowired
  public MemberIdentityHandlerService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public FirebasePrincipal getCurrentFirebasePrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return (FirebasePrincipal) authentication.getPrincipal();
  }

  public User getLoggedInUser() {
    String firebaseUid = getCurrentFirebasePrincipal().uid();
    Optional<User> user = userRepository.findByFirebaseUid(firebaseUid);
    return user.orElseThrow();
  }

  public String getLoggedInMemberID() {
    return getLoggedInUser().getId();
  }
}
