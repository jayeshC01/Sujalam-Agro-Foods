package com.gryffindor.excalibur.models.db;

import com.gryffindor.excalibur.constants.OrderStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Entity
@Data
@Table(name = "orders")
public class Order implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private String orderId = UUID.randomUUID().toString();

  @Column(name = "order_date", nullable = false, columnDefinition = "datetime")
  private LocalDateTime date;

  @Column(name = "order_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private OrderStatus orderStatus;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
  private User user;

  @Embedded private Address shippingAddress;

  @Column(name = "order_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal orderTotal;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderDetails> orderDetails;
}
