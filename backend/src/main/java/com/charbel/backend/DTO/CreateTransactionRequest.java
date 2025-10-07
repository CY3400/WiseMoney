package com.charbel.backend.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateTransactionRequest {
    private Long category;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String notes;

    public Long getCategory(){ return category; }
    public void setCategory(Long category){ this.category = category; }

    public BigDecimal getAmount(){ return amount; }
    public void setAmount(BigDecimal amount){ this.amount = amount; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
