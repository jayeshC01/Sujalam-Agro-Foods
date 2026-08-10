package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.db.Product;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
  private String id;
  private Product.Category category;
  private String name;
  private String description;
  private String imageUrl;
  private String healthBenefits;
  private BigDecimal price;
  private Integer qty;

  public static ProductResponse from(Product product) {
    return ProductResponse.builder()
        .id(product.getId())
        .category(product.getCategory())
        .name(product.getName())
        .description(product.getDescription())
        .imageUrl(product.getImageUrl())
        .healthBenefits(product.getHealthBenefits())
        .price(product.getPrice())
        .qty(product.getQty())
        .build();
  }
}
