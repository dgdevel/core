package com.github.dgdevel.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Menu {
  @JsonProperty("id")
  private Long id;

  @JsonProperty("function_id")
  private Long functionId;

  @JsonProperty("parent_id")
  private Long parentId;

  @JsonProperty("function")
  private Function function;

  @JsonProperty("children")
  private List<Menu> children;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getFunctionId() {
    return functionId;
  }

  public void setFunctionId(Long functionId) {
    this.functionId = functionId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Function getFunction() {
    return function;
  }

  public void setFunction(Function function) {
    this.function = function;
  }

  public List<Menu> getChildren() {
    if (children == null) {
      children = new ArrayList<>();
    }
    return children;
  }

  public void setChildren(List<Menu> children) {
    this.children = children;
  }

  public void addChild(Menu child) {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
    this.children.add(child);
  }
}
