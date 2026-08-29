package com.gryffindor.excalibur.model.request;

import com.gryffindor.excalibur.model.db.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
    description = "Request body payload for creating a new product",
    example =
        "{\"category\":\"EDIBLE\",\"name\":\"Cold-Pressed Groundnut Oil (1L)\",\"description\":\"100% pure cold-pressed groundnut oil.\",\"imageUrl\":\"https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5\",\"healthBenefits\":\"Rich in antioxidants and monounsaturated fats.\",\"price\":240.00,\"qty\":50,\"gstRate\":0.05}")
public class ProductRequest {
  @NotNull(message = "Category cannot be null")
  @Schema(
      description = "Product category (EDIBLE or NOT_EDIBLE)",
      example = "EDIBLE",
      allowableValues = {"EDIBLE", "NOT_EDIBLE"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Product.Category category;

  @NotBlank(message = "Name cannot be empty")
  @Schema(
      description = "Unique product title/name",
      example = "Cold-Pressed Groundnut Oil (1L)",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(
      description = "Detailed product description (Optional)",
      example = "100% pure cold-pressed groundnut oil.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String description;

  @NotBlank(message = "Image URL cannot be empty")
  @Pattern(regexp = "^https?://.*", message = "Image URL must be a valid HTTP or HTTPS URL")
  @Schema(
      description = "Publicly accessible image HTTP/HTTPS URL",
      example = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String imageUrl;

  @Schema(
      description = "Health benefits bullet points or summary (Optional)",
      example = "Rich in antioxidants and monounsaturated fats.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String healthBenefits;

  @NotNull(message = "Price of an item cannot be null")
  @DecimalMin(value = "0.0", message = "Price cannot be negative")
  @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
  @Schema(
      description = "Unit price in INR (must be non-negative)",
      example = "240.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal price;

  @NotNull(message = "Quantity cannot be null")
  @Min(value = 0, message = "Quantity cannot be negative")
  @Schema(
      description = "Initial inventory stock count (must be non-negative)",
      example = "50",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer qty;

  @NotNull(message = "GST rate cannot be null")
  @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
  @DecimalMax(value = "1.0", message = "GST rate cannot exceed 1.0 (100%)")
  @Schema(
      description = "Applicable GST tax rate between 0.0 and 1.0 (e.g. 0.05 for 5%)",
      example = "0.05",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal gstRate;

  public Product toProduct() {
    return Product.builder()
        .category(category)
        .name(name)
        .description(description)
        .imageUrl(imageUrl)
        .healthBenefits(healthBenefits)
        .price(price)
        .qty(qty)
        .gstRate(gstRate)
        .build();
  }
}
