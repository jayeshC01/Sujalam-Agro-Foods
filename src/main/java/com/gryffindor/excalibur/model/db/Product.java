package com.gryffindor.excalibur.model.db;

import com.gryffindor.excalibur.model.common.AuditStamp;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(
    name = "products",
    indexes = {@Index(name = "idx_products_category", columnList = "category")})
public class Product extends AuditStamp implements Serializable {
  public enum Category {
    EDIBLE,
    NOT_EDIBLE
  }

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "category", nullable = false)
  @Enumerated(EnumType.STRING)
  private Category category;

  @Column(name = "name", nullable = false, unique = true)
  @NotBlank(message = "Name cannot be empty")
  private String name;

  @Column(name = "description", length = 2000)
  private String description;

  @Column(name = "image_url", nullable = false, length = 500)
  @NotBlank(message = "Image URL cannot be empty")
  private String imageUrl;

  @Column(name = "health_benefits", length = 2000)
  private String healthBenefits;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  @NotNull(message = "Price of an item cannot be null")
  @DecimalMin(value = "0.0", message = "Price cannot be negative")
  private BigDecimal price;

  @Column(name = "quantity", nullable = false)
  @NotNull(message = "Quantity cannot be null")
  @Min(value = 0, message = "Quantity cannot be negative")
  private Integer qty;
}
