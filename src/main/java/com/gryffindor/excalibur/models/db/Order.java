package com.gryffindor.excalibur.models.db;

import com.gryffindor.excalibur.constants.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "orders")
public class Order implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private String orderId = UUID.randomUUID().toString();

  @Column(name = "order_date", nullable = false)
  private Date date;

  @Column(name = "order_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private OrderStatus orderStatus;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "customer_id", referencedColumnName ="id", nullable = false)
  private User user;

  @Column(name = "order_total", nullable = false)
  private Long orderTotal;

  @OneToMany(cascade = CascadeType.ALL, targetEntity = OrderDetails.class)
  @JoinColumn(name = "order_id", referencedColumnName = "id")
  private List<OrderDetails> orderDetails;
}
