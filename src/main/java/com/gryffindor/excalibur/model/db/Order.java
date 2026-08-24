package com.gryffindor.excalibur.model.db;

import com.gryffindor.excalibur.model.common.AuditStamp;
import com.gryffindor.excalibur.model.constants.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(
    name = "orders",
    indexes = {
      @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
      @Index(name = "idx_orders_status", columnList = "order_status"),
      @Index(name = "idx_orders_created_at", columnList = "created_at")
    })
public class Order extends AuditStamp implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private String orderId;

  @Column(name = "order_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private OrderStatus orderStatus;

  @ManyToOne
  @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
  private User user;

  @Column(name = "sub_total", precision = 12, scale = 2)
  private BigDecimal subTotal;

  @Column(name = "tax_amount", precision = 12, scale = 2)
  private BigDecimal taxAmount;

  @Column(name = "delivery_charge", precision = 12, scale = 2)
  private BigDecimal deliveryCharge;

  @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
  @NotNull(message = "Grand total cannot be null")
  @DecimalMin(value = "0.0", message = "Grand total cannot be negative")
  private BigDecimal grandTotal;

  @Embedded
  @NotNull(message = "Shipping address is required")
  @Valid
  @AttributeOverrides({
    @AttributeOverride(
        name = "recipientName",
        column = @Column(name = "shipping_recipient_name", nullable = false)),
    @AttributeOverride(
        name = "phoneNumber",
        column = @Column(name = "shipping_phone_number", nullable = false)),
    @AttributeOverride(
        name = "addressLine1",
        column = @Column(name = "shipping_address_line1", nullable = false)),
    @AttributeOverride(name = "addressLine2", column = @Column(name = "shipping_address_line2")),
    @AttributeOverride(name = "city", column = @Column(name = "shipping_city", nullable = false)),
    @AttributeOverride(name = "state", column = @Column(name = "shipping_state", nullable = false)),
    @AttributeOverride(
        name = "postalCode",
        column = @Column(name = "shipping_postal_code", nullable = false)),
    @AttributeOverride(
        name = "country",
        column = @Column(name = "shipping_country", nullable = false))
  })
  private Address shippingAddress;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderDetails> orderDetails;
}
