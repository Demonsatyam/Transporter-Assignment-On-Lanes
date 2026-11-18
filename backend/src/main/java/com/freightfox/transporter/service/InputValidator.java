package com.freightfox.transporter.service;

import com.freightfox.transporter.dto.request.LaneQuoteRequest;
import com.freightfox.transporter.dto.request.LaneRequest;
import com.freightfox.transporter.dto.request.TransporterInputRequest;
import com.freightfox.transporter.dto.request.TransporterRequest;
import com.freightfox.transporter.exception.InvalidInputException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cross-field rules bean validation cannot express. Field-level rules (required, positive, sizes) are already
 * enforced by annotations on the request DTOs before this runs. Reports every error, not just the first.
 */
@Component
public class InputValidator {

    public void validate(TransporterInputRequest request) {
        List<String> errors = new ArrayList<>();

        Set<Long> laneIds = new HashSet<>();
        for (LaneRequest lane : request.lanes()) {
            if (!laneIds.add(lane.id())) {
                errors.add("lanes: duplicate lane id " + lane.id());
            }
        }

        Set<Long> transporterIds = new HashSet<>();
        for (TransporterRequest transporter : request.transporters()) {
            if (!transporterIds.add(transporter.id())) {
                errors.add("transporters: duplicate transporter id " + transporter.id());
            }
            Set<Long> quotedLanes = new HashSet<>();
            for (LaneQuoteRequest quote : transporter.laneQuotes()) {
                if (!laneIds.contains(quote.laneId())) {
                    errors.add("transporter " + transporter.id() + ": lane " + quote.laneId() + " does not exist");
                }
                if (!quotedLanes.add(quote.laneId())) {
                    errors.add("transporter " + transporter.id() + ": duplicate quote for lane " + quote.laneId());
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidInputException(errors);
        }
    }
}
