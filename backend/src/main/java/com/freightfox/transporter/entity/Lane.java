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

@Entity
@Table(name = "lane")
public class Lane {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lane_seq")
    @SequenceGenerator(name = "lane_seq", sequenceName = "lane_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private InputDataset dataset;

    /** Lane id as supplied by the client; unique within its dataset. */
    @Column(name = "lane_ref", nullable = false)
    private Long laneRef;

    @Column(nullable = false, length = 100)
    private String origin;

    @Column(nullable = false, length = 100)
    private String destination;

    protected Lane() {
    }

    public Lane(Long laneRef, String origin, String destination) {
        this.laneRef = laneRef;
        this.origin = origin;
        this.destination = destination;
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

    public Long getLaneRef() {
        return laneRef;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }
}
