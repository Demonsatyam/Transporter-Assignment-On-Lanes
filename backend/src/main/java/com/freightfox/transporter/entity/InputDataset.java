package com.freightfox.transporter.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** One submitted input (lanes + transporters + quotes). Each submission is isolated from the others. */
@Entity
@Table(name = "input_dataset")
public class InputDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "input_dataset_seq")
    @SequenceGenerator(name = "input_dataset_seq", sequenceName = "input_dataset_seq", allocationSize = 1)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lane> lanes = new ArrayList<>();

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transporter> transporters = new ArrayList<>();

    protected InputDataset() {
    }

    public InputDataset(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void addLane(Lane lane) {
        lanes.add(lane);
        lane.setDataset(this);
    }

    public void addTransporter(Transporter transporter) {
        transporters.add(transporter);
        transporter.setDataset(this);
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Lane> getLanes() {
        return lanes;
    }

    public List<Transporter> getTransporters() {
        return transporters;
    }
}
