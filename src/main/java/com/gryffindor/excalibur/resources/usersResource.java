package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.SimpleResponse;
import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.services.usersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class usersResource {
  private final usersService usersService;

  @Autowired
  usersResource(usersService usersService) {
    this.usersService = usersService;
  }

  @GetMapping("/customer/{id}")
  public SimpleResponse<User> getCustomer(@PathVariable String id) {
    return usersService.getCustomer(id);
  }

  @GetMapping("/customers")
  public SimpleResponse<List<User>> getCustomers() {
    return usersService.getAllCustomers();
  }

  @PostMapping("/customer/register")
  public SimpleResponse<String> registerCustomer(@RequestBody RegisterUser user) {
    return usersService.addUser(user, Roles.USER);
  }

  @PostMapping("/admin/register")
  public SimpleResponse<String> registerAdmin(@RequestBody RegisterUser user) {
    return usersService.addUser(user, Roles.ADMIN);
  }

  /**
  @PostMapping("/authenticate")
  public ResponseEntity<String> authenticateCustomer(@RequestBody AuthenticationRequest authenticationDetails) {
    try {
      Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(authenticationDetails.getUsername(), authenticationDetails.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String token = JwtUtils.generateToken(authentication);
    return ResponseEntity.ok(token);
  } catch (BadCredentialsException e) {
    return new ResponseEntity<>("Invalid password or error", HttpStatus.BAD_REQUEST);
    }
  }
  */
}
