package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.models.db.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByUserName(String userName);
}
