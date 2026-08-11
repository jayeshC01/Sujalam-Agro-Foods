package com.gryffindor.excalibur.model.db;

import com.gryffindor.excalibur.model.common.AuditStamp;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(
    name = "order_details",
    indexes = {@Index(name = "idx_order_details_order_id", columnList = "order_id")})
public class OrderDetails extends AuditStamp {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private String id;

  @ManyToOne
  @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Order order;

  @ManyToOne
  @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
  private Product product;

  @Column(name = "ordered_qty", nullable = false)
  @NotNull(message = "Ordered quantity cannot be null")
  @Min(value = 1, message = "Ordered quantity must be at least 1")
  private Integer orderedQty;

  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  @NotNull(message = "Unit price cannot be null")
  @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
  private BigDecimal unitPrice;

  @Column(name = "sub_total", nullable = false, precision = 12, scale = 2)
  @NotNull(message = "Sub total cannot be null")
  @DecimalMin(value = "0.0", message = "Sub total cannot be negative")
  private BigDecimal subTotal;
}
