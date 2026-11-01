package com.smartbank.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminActivitySummary {

    private long transactionCount;
    private BigDecimal creditVolume;
    private BigDecimal debitVolume;
    private BigDecimal transferVolume;
}
