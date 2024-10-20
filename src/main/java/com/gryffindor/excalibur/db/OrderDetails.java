package com.gryffindor.excalibur.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "order_details")
public class OrderDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private String id = UUID.randomUUID().toString();

  @ManyToOne
  @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
  private Product product;

  @Column(name = "quantity")
  private Integer quantity;

  @Column(name= "sub_total")
  private Long total;
}
