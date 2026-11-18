package com.freightfox.transporter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "lane_quote")
public class LaneQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lane_quote_seq")
    @SequenceGenerator(name = "lane_quote_seq", sequenceName = "lane_quote_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lane_id", nullable = false)
    private Lane lane;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quote;

    protected LaneQuote() {
    }

    public LaneQuote(Lane lane, BigDecimal quote) {
        this.lane = lane;
        this.quote = quote;
    }

    void setTransporter(Transporter transporter) {
        this.transporter = transporter;
    }

    public Long getId() {
        return id;
    }

    public Lane getLane() {
        return lane;
    }

    public Transporter getTransporter() {
        return transporter;
    }

    public BigDecimal getQuote() {
        return quote;
    }
}
