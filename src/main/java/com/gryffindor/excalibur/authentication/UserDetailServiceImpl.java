//package com.gryffindor.excalibur.authentication;
//
//import com.gryffindor.excalibur.models.db.User;
//import com.gryffindor.excalibur.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import java.util.Optional;
//
//@Service
//public class UserDetailServiceImpl implements UserDetailsService {
//
//  @Autowired
//  UserRepository userRepository;
//
//  @Override
//  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//    Optional<User> user = userRepository.findByUserName(username);
//    return user.map(UserDetailsImpl::new)
//            .orElseThrow(() -> new UsernameNotFoundException(username));
//  }
//}
