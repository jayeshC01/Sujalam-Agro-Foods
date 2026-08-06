package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.models.db.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
  List<Order> getOrderByUserId(String customerId);
}
