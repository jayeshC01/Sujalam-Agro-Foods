package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.config.FirebasePrincipal;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.exception.AccountDisabledException;
import com.gryffindor.excalibur.model.exception.UserNotRegisteredException;
import com.gryffindor.excalibur.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
    User user =
        userRepository
            .findByFirebaseUid(firebaseUid)
            .orElseThrow(
                () ->
                    new UserNotRegisteredException(
                        "No profile found for this account. Please complete registration first."));
    if (user.getStatus() == User.Status.BLOCKED) {
      throw new AccountDisabledException(
          "Your account has been blocked by an administrator. Please contact support.");
    }
    if (!user.isActive()) {
      throw new AccountDisabledException(
          "Your account has been deactivated. Please contact support.");
    }
    return user;
  }

  public String getLoggedInMemberID() {
    return getLoggedInUser().getId();
  }

  public boolean isAdmin() {
    return getLoggedInUser().getRole() == Roles.ADMIN;
  }

  public boolean isOwner(String ownerId) {
    return ownerId != null && ownerId.equals(getLoggedInMemberID());
  }

  public User requireAdmin() throws AccessDeniedException {
    User currentUser = getLoggedInUser();
    if (!isAdmin()) {
      throw new AccessDeniedException("You are not allowed to access this resource");
    }
    return currentUser;
  }
}
