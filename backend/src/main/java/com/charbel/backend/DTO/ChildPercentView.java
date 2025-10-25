package com.charbel.backend.DTO;

import java.math.BigDecimal;

public interface ChildPercentView {
  String getName();
  BigDecimal getAmount();
  BigDecimal getTotal();
  Integer getPercent();
}