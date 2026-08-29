package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.db.Order;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
  Page<Order> findByUserId(String userId, Pageable pageable);

  @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
  Optional<Order> findByUserIdAndIdempotencyKey(String customerId, String idempotencyKey);

  @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
  @Query(
      "SELECT o FROM Order o WHERE "
          + "(:status IS NULL OR o.orderStatus = :status) "
          + "AND (:customerId IS NULL OR TRIM(:customerId) = '' OR o.user.id = :customerId) "
          + "AND (:startDate IS NULL OR o.createdAt >= :startDate) "
          + "AND (:endDate IS NULL OR o.createdAt <= :endDate)")
  Page<Order> searchAdminOrders(
      @Param("status") OrderStatus status,
      @Param("customerId") String customerId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);
}
