package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class MemberIdentityHandlerService {
  private final CustomerRepository customerRepository;

  @Autowired
  public MemberIdentityHandlerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public String getLoggedInMemberID() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    Optional<Customer> user = customerRepository.findByUserName(username);
    return user.map(Customer::getId).orElseThrow();

  }
}
