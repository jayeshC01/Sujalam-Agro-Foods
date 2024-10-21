package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.db.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
  Optional<Customer> findByUserName(String userName);
}
