package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.models.db.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
   List<Order> getOrderByUserId(String customerId);
}
