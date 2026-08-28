package com.gryffindor.excalibur.model.request;

import com.gryffindor.excalibur.model.db.Product;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
  @NotNull(message = "Category cannot be null")
  private Product.Category category;

  @NotBlank(message = "Name cannot be empty")
  private String name;

  private String description;

  @NotBlank(message = "Image URL cannot be empty")
  @Pattern(regexp = "^https?://.*", message = "Image URL must be a valid HTTP or HTTPS URL")
  private String imageUrl;

  private String healthBenefits;

  @NotNull(message = "Price of an item cannot be null")
  @DecimalMin(value = "0.0", message = "Price cannot be negative")
  @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
  private BigDecimal price;

  @NotNull(message = "GST rate cannot be null")
  @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
  @DecimalMax(value = "1.0", message = "GST rate cannot exceed 1.0 (100%)")
  private BigDecimal gstRate;
}
