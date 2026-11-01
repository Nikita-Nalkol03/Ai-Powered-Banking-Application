package com.smartbank.repository;

import java.math.BigDecimal;

public interface AdminActivityProjection {

    Long getTransactionCount();

    BigDecimal getCreditVolume();

    BigDecimal getDebitVolume();

    BigDecimal getTransferVolume();
}
