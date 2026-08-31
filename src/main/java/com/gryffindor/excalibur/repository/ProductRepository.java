package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.model.db.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
  Optional<Product> findByIdAndStatus(String id, Product.Status status);

  @Query(
      "SELECT p FROM Product p WHERE p.status = 'ACTIVE' "
          + "AND (:category IS NULL OR p.category = :category) "
          + "AND (:query IS NULL OR TRIM(:query) = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', TRIM(:query), '%')))")
  Page<Product> searchPublicProducts(
      @Param("category") Product.Category category,
      @Param("query") String query,
      Pageable pageable);

  @Query(
      "SELECT p FROM Product p WHERE "
          + "(:status IS NULL OR p.status = :status) "
          + "AND (:category IS NULL OR p.category = :category) "
          + "AND (:query IS NULL OR TRIM(:query) = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', TRIM(:query), '%')))")
  Page<Product> searchAdminProducts(
      @Param("status") Product.Status status,
      @Param("category") Product.Category category,
      @Param("query") String query,
      Pageable pageable);

  @Modifying(clearAutomatically = true)
  @Query(
      "UPDATE Product p SET p.qty = p.qty - :quantity, p.updatedAt = CURRENT_TIMESTAMP, p.version = COALESCE(p.version, 0) + 1 WHERE p.id = :id AND p.qty >= :quantity")
  int decrementStock(@Param("id") String id, @Param("quantity") int quantity);

  @Modifying(clearAutomatically = true)
  @Query(
      "UPDATE Product p SET p.qty = p.qty + :quantity, p.updatedAt = CURRENT_TIMESTAMP, p.version = COALESCE(p.version, 0) + 1 WHERE p.id = :id AND p.status = 'ACTIVE'")
  int incrementStock(@Param("id") String id, @Param("quantity") int quantity);
}
