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
import org.hibernate.annotations.Check;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
  @Check(name = "chk_products_name_not_blank", constraints = "TRIM(name) <> ''")
  private String name;

  @Column(name = "description", length = 2000)
  private String description;

  @Column(name = "image_url", nullable = false, length = 500)
  @Check(name = "chk_products_image_url_not_blank", constraints = "TRIM(image_url) <> ''")
  private String imageUrl;

  @Column(name = "health_benefits", length = 2000)
  private String healthBenefits;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  @Check(name = "chk_products_price_non_negative", constraints = "price >= 0")
  private BigDecimal price;

  @Column(name = "quantity", nullable = false)
  @Check(name = "chk_products_quantity_non_negative", constraints = "quantity >= 0")
  private Integer qty;

  @Column(name = "gst_rate", nullable = false, precision = 5, scale = 4)
  @Check(name = "chk_products_gst_rate_non_negative", constraints = "gst_rate >= 0")
  private BigDecimal gstRate;
}
