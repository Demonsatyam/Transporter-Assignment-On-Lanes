package com.freightfox.transporter.service;

import com.freightfox.transporter.dto.request.TransporterInputRequest;
import com.freightfox.transporter.entity.InputDataset;
import com.freightfox.transporter.mapper.TransporterMapper;
import com.freightfox.transporter.repository.InputDatasetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TransporterInputService {

    private static final Logger log = LoggerFactory.getLogger(TransporterInputService.class);

    private final InputValidator validator;
    private final TransporterMapper mapper;
    private final InputDatasetRepository datasetRepository;

    public TransporterInputService(InputValidator validator, TransporterMapper mapper,
                                   InputDatasetRepository datasetRepository) {
        this.validator = validator;
        this.mapper = mapper;
        this.datasetRepository = datasetRepository;
    }

    /** Saves the whole submission as a new dataset, all-or-nothing. Returns the new dataset id. */
    @Transactional
    public Long save(TransporterInputRequest request) {
        validator.validate(request);
        InputDataset dataset = datasetRepository.save(mapper.toDataset(request, Instant.now()));
        log.info("Saved dataset {}: {} lanes, {} transporters", dataset.getId(),
                request.lanes().size(), request.transporters().size());
        return dataset.getId();
    }
}
