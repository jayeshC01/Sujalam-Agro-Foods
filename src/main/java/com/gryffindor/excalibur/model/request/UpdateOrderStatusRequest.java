package com.gryffindor.excalibur.model.request;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for updating order status")
public class UpdateOrderStatusRequest {
  @NotNull(message = "Order status is required")
  @Schema(
      description = "Target order status (PENDING, COMPLETED, CANCELED)",
      example = "COMPLETED",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private OrderStatus status;
}
