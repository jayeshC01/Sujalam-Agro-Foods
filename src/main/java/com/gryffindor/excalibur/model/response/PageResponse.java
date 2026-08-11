package com.gryffindor.excalibur.model.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean first;
  private boolean last;

  public boolean getFirst() {
    return first;
  }

  public boolean getLast() {
    return last;
  }
}
