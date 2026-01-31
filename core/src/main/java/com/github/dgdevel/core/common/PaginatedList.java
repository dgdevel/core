package com.github.dgdevel.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedList<T> {
  @JsonProperty("page")
  private List<T> page;

  @JsonProperty("totalCount")
  private int totalCount;

  public List<T> getPage() {
    return page;
  }

  public void setPage(List<T> page) {
    this.page = page;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }
}
