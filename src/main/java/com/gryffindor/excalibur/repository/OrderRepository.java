package com.gryffindor.excalibur.repository;

import com.gryffindor.excalibur.db.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
   List<Order> getOrderByCustomerId(String customerId);
}
