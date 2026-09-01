package com.travelmate.service;

public enum StockPreDeductResult {
    DEDUCTED_IN_REDIS,
    FALLBACK_TO_DB,
    NO_STOCK
}
