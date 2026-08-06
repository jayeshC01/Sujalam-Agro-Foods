package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.models.db.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
  Optional<Product> findByName(String name);
}
