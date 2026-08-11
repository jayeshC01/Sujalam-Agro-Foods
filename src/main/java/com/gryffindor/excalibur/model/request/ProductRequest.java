package com.gryffindor.excalibur.model.request;

import com.gryffindor.excalibur.model.db.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductRequest {
  @NotNull(message = "Category cannot be null")
  private Product.Category category;

  @NotBlank(message = "Name cannot be empty")
  private String name;

  private String description;

  @NotBlank(message = "Image URL cannot be empty")
  private String imageUrl;

  private String healthBenefits;

  @NotNull(message = "Price of an item cannot be null")
  @DecimalMin(value = "0.0", message = "Price cannot be negative")
  private BigDecimal price;

  @NotNull(message = "Quantity cannot be null")
  @Min(value = 0, message = "Quantity cannot be negative")
  private Integer qty;

  public Product toProduct() {
    return Product.builder()
        .category(category)
        .name(name)
        .description(description)
        .imageUrl(imageUrl)
        .healthBenefits(healthBenefits)
        .price(price)
        .qty(qty)
        .build();
  }
}
