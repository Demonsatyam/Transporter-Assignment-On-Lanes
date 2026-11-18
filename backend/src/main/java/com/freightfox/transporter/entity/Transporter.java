package com.freightfox.transporter.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transporter")
public class Transporter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transporter_seq")
    @SequenceGenerator(name = "transporter_seq", sequenceName = "transporter_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private InputDataset dataset;

    /** Transporter id as supplied by the client; unique within its dataset. */
    @Column(name = "transporter_ref", nullable = false)
    private Long transporterRef;

    @Column(nullable = false, length = 150)
    private String name;

    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LaneQuote> laneQuotes = new ArrayList<>();

    protected Transporter() {
    }

    public Transporter(Long transporterRef, String name) {
        this.transporterRef = transporterRef;
        this.name = name;
    }

    public void addQuote(LaneQuote quote) {
        laneQuotes.add(quote);
        quote.setTransporter(this);
    }

    void setDataset(InputDataset dataset) {
        this.dataset = dataset;
    }

    public Long getId() {
        return id;
    }

    public InputDataset getDataset() {
        return dataset;
    }

    public Long getTransporterRef() {
        return transporterRef;
    }

    public String getName() {
        return name;
    }

    public List<LaneQuote> getLaneQuotes() {
        return laneQuotes;
    }
}
