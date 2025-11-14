package com.charbel.backend.DTO;

import java.math.BigDecimal;

public interface StatisticsViews {
    Integer getMonth();
    Integer getYear();
    BigDecimal getExpenses();
    BigDecimal getRevenues();
    BigDecimal getSavings();
}
