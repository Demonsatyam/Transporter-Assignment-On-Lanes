package com.freightfox.transporter.mapper;

import com.freightfox.transporter.dto.request.LaneRequest;
import com.freightfox.transporter.dto.request.TransporterInputRequest;
import com.freightfox.transporter.dto.request.TransporterRequest;
import com.freightfox.transporter.dto.response.AssignmentDetailResponse;
import com.freightfox.transporter.dto.response.AssignmentResponse;
import com.freightfox.transporter.entity.InputDataset;
import com.freightfox.transporter.entity.Lane;
import com.freightfox.transporter.entity.LaneQuote;
import com.freightfox.transporter.entity.Transporter;
import com.freightfox.transporter.optimization.OptimizationResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class TransporterMapper {

    /** Builds the full entity graph; quote lane references must already be validated. */
    public InputDataset toDataset(TransporterInputRequest request, Instant createdAt) {
        InputDataset dataset = new InputDataset(createdAt);
        Map<Long, Lane> lanesByRef = new HashMap<>();
        for (LaneRequest laneRequest : request.lanes()) {
            Lane lane = new Lane(laneRequest.id(), laneRequest.origin().trim(), laneRequest.destination().trim());
            lanesByRef.put(lane.getLaneRef(), lane);
            dataset.addLane(lane);
        }
        for (TransporterRequest transporterRequest : request.transporters()) {
            Transporter transporter = new Transporter(transporterRequest.id(), transporterRequest.name().trim());
            transporterRequest.laneQuotes().forEach(quote ->
                    transporter.addQuote(new LaneQuote(lanesByRef.get(quote.laneId()), quote.quote())));
            dataset.addTransporter(transporter);
        }
        return dataset;
    }

    public AssignmentResponse toResponse(OptimizationResult result) {
        return new AssignmentResponse(
                "success",
                result.totalCost(),
                result.assignments().stream()
                        .map(a -> new AssignmentDetailResponse(a.laneId(), a.transporterId()))
                        .toList(),
                result.selectedTransporters());
    }
}
