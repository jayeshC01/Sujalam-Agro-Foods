package com.gryffindor.excalibur.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Standard paginated wrapper response")
public class PageResponse<T> {
  @Schema(
      description = "List of items in the current page",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<T> content;

  @Schema(
      description = "Current page index (0-based)",
      example = "0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int page;

  @Schema(
      description = "Number of items per page",
      example = "10",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int size;

  @Schema(
      description = "Total number of items across all pages",
      example = "42",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private long totalElements;

  @Schema(
      description = "Total number of pages",
      example = "5",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int totalPages;

  @Schema(
      description = "True if this is the first page",
      example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean first;

  @Schema(
      description = "True if this is the last page",
      example = "false",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean last;

  public boolean getFirst() {
    return first;
  }

  public boolean getLast() {
    return last;
  }
}
