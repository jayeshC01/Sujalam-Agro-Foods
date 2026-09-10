package com.gryffindor.excalibur.model.db;

import com.gryffindor.excalibur.model.common.AuditStamp;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = "products")
public class Product extends AuditStamp implements Serializable {
  public enum Category {
    EDIBLE,
    NOT_EDIBLE
  }

  public enum Status {
    ACTIVE,
    INACTIVE
  }

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(name = "category", nullable = false)
  @Enumerated(EnumType.STRING)
  private Category category;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "description", length = 2000)
  private String description;

  @Column(name = "image_url", nullable = false, length = 500)
  private String imageUrl;

  @Column(name = "health_benefits", length = 2000)
  private String healthBenefits;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "quantity", nullable = false)
  private Integer qty;

  @Column(name = "gst_rate", nullable = false, precision = 5, scale = 4)
  private BigDecimal gstRate;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Status status = Status.ACTIVE;
}
