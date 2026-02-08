package com.charbel.backend.DTO;

import java.math.BigDecimal;

public interface ExpDiffDTO {
    String getName();
    BigDecimal getTotal();
    BigDecimal getCurrent();
    BigDecimal getOld();
}