package com.charbel.backend.DTO;

import java.math.BigDecimal;

public class UpdateBudgetRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
