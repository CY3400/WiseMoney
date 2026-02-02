package com.charbel.backend.DTO;

import java.math.BigDecimal;

import com.charbel.backend.model.TypeAllocation;

public class CreateBudgetManagementRequest {
    private Long category;
    private BigDecimal amount;
    private TypeAllocation type;
    private int frequency;

    public Long getCategory(){ return category; }
    public void setCategory(Long category){ this.category = category; }

    public TypeAllocation getType(){ return type; }
    public void setType(TypeAllocation typeAllocation){ this.type = typeAllocation; }

    public BigDecimal getAmount(){ return amount; }
    public void setAmount(BigDecimal amount){ this.amount = amount; }

    public int getFrequency(){ return frequency; }
    public void setFrequency(int frequency){ this.frequency = frequency; }
}
