package com.gryffindor.excalibur.db;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "order_details")
public class OrderDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id = UUID.randomUUID().toString();

  @ManyToOne
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "quantity")
  private Integer quantity;

  @Column(name= "sub_total", nullable = false)
  private Long total;
}
