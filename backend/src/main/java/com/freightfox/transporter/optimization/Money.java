package com.freightfox.transporter.optimization;

import java.math.BigDecimal;

/** Money helpers. Quotes carry at most 2 decimal places, so integer paise are exact. */
public final class Money {

    private Money() {
    }

    public static long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    /** 69000.00 -> 69000, 12.50 -> 12.5; never scientific notation. */
    public static BigDecimal normalize(BigDecimal amount) {
        BigDecimal stripped = amount.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
