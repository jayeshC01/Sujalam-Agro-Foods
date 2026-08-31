package com.gryffindor.excalibur.model.db;

import com.gryffindor.excalibur.model.common.AuditStamp;
import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.constants.PaymentMethod;
import com.gryffindor.excalibur.model.constants.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Check;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(
    name = "orders",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_orders_user_idempotency",
          columnNames = {"customer_id", "idempotency_key"})
    },
    indexes = {
      @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
      @Index(name = "idx_orders_idempotency_key", columnList = "idempotency_key"),
      @Index(name = "idx_orders_status", columnList = "order_status"),
      @Index(name = "idx_orders_created_at", columnList = "created_at")
    })
public class Order extends AuditStamp implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private String orderId;

  @Column(name = "idempotency_key", nullable = false, length = 128)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "order_status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Check(
      name = "chk_orders_status_valid",
      constraints = "order_status IN ('PROCESSING', 'PACKED', 'SHIPPED', 'COMPLETED', 'CANCELED')")
  private OrderStatus orderStatus;

  @Column(name = "payment_method", nullable = false)
  @Enumerated(EnumType.STRING)
  @Check(
      name = "chk_orders_payment_method_valid",
      constraints = "payment_method IN ('COD', 'UPI', 'CARD')")
  private PaymentMethod paymentMethod = PaymentMethod.COD;

  @Column(name = "payment_status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Check(
      name = "chk_orders_payment_status_valid",
      constraints = "payment_status IN ('PENDING', 'PAID', 'FAILED')")
  private PaymentStatus paymentStatus = PaymentStatus.PENDING;

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
