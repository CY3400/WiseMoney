package com.charbel.backend.DTO;

public class UpdateCategoryRequest {
    private Long parentId;
    private String name;

    public Long getParentId(){ return parentId; }
    public void setParentId(Long parentId){ this.parentId = parentId; }

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
}
