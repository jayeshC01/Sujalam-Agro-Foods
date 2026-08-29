package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.model.db.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByFirebaseUid(String firebaseUid);

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailAndStatus(String email, User.Status status);

  @Query(
      "SELECT u FROM User u WHERE "
          + "(:status IS NULL OR u.status = :status) "
          + "AND (:query IS NULL OR TRIM(:query) = '' "
          + "     OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', TRIM(:query), '%')) "
          + "     OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', TRIM(:query), '%')) "
          + "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', TRIM(:query), '%')) "
          + "     OR u.phoneNumber LIKE CONCAT('%', TRIM(:query), '%'))")
  Page<User> searchCustomers(
      @Param("status") User.Status status, @Param("query") String query, Pageable pageable);
}
