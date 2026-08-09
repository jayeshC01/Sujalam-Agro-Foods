package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.model.db.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByFirebaseUid(String firebaseUid);

  Optional<User> findByEmail(String email);
}
