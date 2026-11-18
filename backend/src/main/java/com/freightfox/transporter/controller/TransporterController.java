package com.freightfox.transporter.controller;

import com.freightfox.transporter.dto.request.AssignmentRequest;
import com.freightfox.transporter.dto.request.TransporterInputRequest;
import com.freightfox.transporter.dto.response.ApiErrorResponse;
import com.freightfox.transporter.dto.response.AssignmentResponse;
import com.freightfox.transporter.dto.response.InputResponse;
import com.freightfox.transporter.service.TransporterAssignmentService;
import com.freightfox.transporter.service.TransporterInputService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transporters")
@Tag(name = "Transporter assignment")
public class TransporterController {

    private final TransporterInputService inputService;
    private final TransporterAssignmentService assignmentService;

    public TransporterController(TransporterInputService inputService,
                                 TransporterAssignmentService assignmentService) {
        this.inputService = inputService;
        this.assignmentService = assignmentService;
    }

    @PostMapping("/input")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit lanes, transporters and their lane quotes",
            description = "Saves the submission as a new, isolated dataset. Nothing is saved if any part is invalid.")
    @ApiResponse(responseCode = "201", description = "Saved")
    @ApiResponse(responseCode = "400", description = "Invalid input (missing fields, quote <= 0, duplicate ids, "
            + "quote for an unknown lane)", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public InputResponse submitInput(@Valid @RequestBody TransporterInputRequest request) {
        return InputResponse.saved(inputService.save(request));
    }

    @PostMapping("/assignment")
    @Operation(summary = "Compute the optimal transporter assignment",
            description = "Assigns every lane to exactly one transporter that quoted for it, using at most "
                    + "maxTransporters distinct transporters. Default policy: use as many transporters as possible, "
                    + "then minimize total cost. Uses the latest dataset unless datasetId is given.")
    @ApiResponse(responseCode = "200", description = "Optimal assignment")
    @ApiResponse(responseCode = "400", description = "Invalid maxTransporters",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No dataset / unknown datasetId",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "No assignment covers every lane within the limit",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Solver time limit exceeded",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public AssignmentResponse getAssignment(@Valid @RequestBody AssignmentRequest request) {
        return assignmentService.assign(request);
    }
}
