package com.charbel.backend.DTO;

import com.charbel.backend.model.CategoryType;

public class CreateCategoryRequest {
    private CategoryType type;
    private Long parentId;
    private String name;
    
    public CategoryType getType(){ return type; }
    public void setType(CategoryType type){ this.type = type; }

    public Long getParentId(){ return parentId; }
    public void setParentId(Long parentId){ this.parentId = parentId; }

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
}
