package com.freightfox.transporter.repository;

import com.freightfox.transporter.entity.InputDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InputDatasetRepository extends JpaRepository<InputDataset, Long> {

    @Query("select max(d.id) from InputDataset d")
    Optional<Long> findLatestId();
}
