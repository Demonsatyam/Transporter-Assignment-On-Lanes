package com.freightfox.transporter.service;

import com.freightfox.transporter.config.OptimizerProperties;
import com.freightfox.transporter.dto.request.AssignmentRequest;
import com.freightfox.transporter.dto.response.AssignmentResponse;
import com.freightfox.transporter.exception.ResourceNotFoundException;
import com.freightfox.transporter.mapper.TransporterMapper;
import com.freightfox.transporter.optimization.AssignmentOptimizer;
import com.freightfox.transporter.optimization.AssignmentProblem;
import com.freightfox.transporter.optimization.OptimizationResult;
import com.freightfox.transporter.optimization.SolutionVerifier;
import com.freightfox.transporter.repository.InputDatasetRepository;
import com.freightfox.transporter.repository.LaneQuoteRepository;
import com.freightfox.transporter.repository.LaneRepository;
import org.springframework.stereotype.Service;

@Service
public class TransporterAssignmentService {

    private final InputDatasetRepository datasetRepository;
    private final LaneRepository laneRepository;
    private final LaneQuoteRepository quoteRepository;
    private final AssignmentOptimizer optimizer;
    private final OptimizerProperties properties;
    private final TransporterMapper mapper;

    public TransporterAssignmentService(InputDatasetRepository datasetRepository, LaneRepository laneRepository,
                                        LaneQuoteRepository quoteRepository, AssignmentOptimizer optimizer,
                                        OptimizerProperties properties, TransporterMapper mapper) {
        this.datasetRepository = datasetRepository;
        this.laneRepository = laneRepository;
        this.quoteRepository = quoteRepository;
        this.optimizer = optimizer;
        this.properties = properties;
        this.mapper = mapper;
    }

    /**
     * Not transactional on purpose: datasets are immutable once saved, so two plain reads are consistent,
     * and no DB connection is held while the solver runs.
     */
    public AssignmentResponse assign(AssignmentRequest request) {
        Long datasetId = resolveDatasetId(request.datasetId());
        AssignmentProblem problem = new AssignmentProblem(
                laneRepository.findLaneRefsByDatasetId(datasetId),
                quoteRepository.findQuotesByDatasetId(datasetId),
                request.maxTransporters());

        OptimizationResult result = optimizer.optimize(problem, properties.policy());
        SolutionVerifier.verify(problem, result);
        return mapper.toResponse(result);
    }

    private Long resolveDatasetId(Long requested) {
        if (requested == null) {
            return datasetRepository.findLatestId()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No input data found. Submit data via POST /api/v1/transporters/input first."));
        }
        if (!datasetRepository.existsById(requested)) {
            throw new ResourceNotFoundException("Dataset " + requested + " not found.");
        }
        return requested;
    }
}
