package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.db.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Product details response representation")
public class ProductResponse {
  @Schema(
      description = "Unique product identifier (UUID)",
      example = "550e8400-e29b-41d4-a716-446655440000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Schema(
      description = "Product category (EDIBLE, NOT_EDIBLE)",
      example = "EDIBLE",
      allowableValues = {"EDIBLE", "NOT_EDIBLE"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Product.Category category;

  @Schema(
      description = "Product name",
      example = "Cold-Pressed Groundnut Oil (1L)",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(
      description = "Product description (Nullable/Optional)",
      example = "100% pure cold-pressed groundnut oil.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String description;

  @Schema(
      description = "Product image URL",
      example = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String imageUrl;

  @Schema(
      description = "Health benefits information (Nullable/Optional)",
      example = "Rich in antioxidants and monounsaturated fats.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String healthBenefits;

  @Schema(
      description = "Unit price in INR",
      example = "240.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal price;

  @Schema(
      description = "Current available stock quantity",
      example = "50",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer qty;

  @Schema(
      description = "Applicable GST tax rate",
      example = "0.05",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal gstRate;

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
        .gstRate(product.getGstRate())
        .build();
  }
}
