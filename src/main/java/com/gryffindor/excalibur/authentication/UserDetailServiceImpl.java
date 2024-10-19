package com.gryffindor.excalibur.authentication;

import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

  @Autowired
  CustomerRepository customerRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<Customer> user = customerRepository.findByUserName(username);
    return user.map(UserDetailsImpl::new)
            .orElseThrow(() -> new UsernameNotFoundException(username));
  }
}
