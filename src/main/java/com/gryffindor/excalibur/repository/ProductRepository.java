package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.db.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
  Optional<Product> findByName(String name);
}
