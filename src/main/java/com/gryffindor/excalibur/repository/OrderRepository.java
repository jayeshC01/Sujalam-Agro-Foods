package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.model.db.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

  @Override
  @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
  Page<Order> findAll(Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
  Optional<Order> findById(String id);

  @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
  List<Order> getOrderByUserId(String customerId);

  @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
  Optional<Order> findByUserIdAndIdempotencyKey(String customerId, String idempotencyKey);
}
