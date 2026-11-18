package com.freightfox.transporter.repository;

import com.freightfox.transporter.entity.Lane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LaneRepository extends JpaRepository<Lane, Long> {

    @Query("select l.laneRef from Lane l where l.dataset.id = :datasetId")
    List<Long> findLaneRefsByDatasetId(Long datasetId);
}
