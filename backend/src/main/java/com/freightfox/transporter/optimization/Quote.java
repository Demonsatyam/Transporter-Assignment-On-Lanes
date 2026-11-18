package com.freightfox.transporter.optimization;

import java.math.BigDecimal;
import java.util.Objects;

/** A transporter's price for one lane. Ids are the client-facing lane/transporter ids. */
public record Quote(long laneId, long transporterId, BigDecimal cost) {

    public Quote {
        Objects.requireNonNull(cost, "cost");
    }
}
