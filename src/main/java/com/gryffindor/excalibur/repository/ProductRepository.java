package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.model.db.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
  Optional<Product> findByName(String name);

  @Modifying
  @Query("UPDATE Product p SET p.qty = p.qty - :quantity WHERE p.id = :id AND p.qty >= :quantity")
  int decrementStock(@Param("id") String id, @Param("quantity") int quantity);
}
