package com.gryffindor.excalibur.models.db;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;

@Entity
@Data
@Table(name = "order_details")
public class OrderDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private String id;

  @ManyToOne
  @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false)
  private Order order;

  @ManyToOne
  @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
  private Product product;

  @Column(name = "quantity")
  private Integer quantity;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "sub_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal subTotal;
}
