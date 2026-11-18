package com.freightfox.transporter.service;

import com.freightfox.transporter.dto.request.LaneQuoteRequest;
import com.freightfox.transporter.dto.request.LaneRequest;
import com.freightfox.transporter.dto.request.TransporterInputRequest;
import com.freightfox.transporter.dto.request.TransporterRequest;
import com.freightfox.transporter.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    private static LaneRequest lane(long id) {
        return new LaneRequest(id, "A", "B");
    }

    private static TransporterRequest transporter(long id, LaneQuoteRequest... quotes) {
        return new TransporterRequest(id, "T" + id, List.of(quotes));
    }

    private static LaneQuoteRequest quote(long laneId) {
        return new LaneQuoteRequest(laneId, BigDecimal.TEN);
    }

    @Test
    void acceptsValidInputIncludingTransporterWithoutQuotes() {
        TransporterInputRequest request = new TransporterInputRequest(List.of(lane(1), lane(2)),
                List.of(transporter(1, quote(1), quote(2)), transporter(2)));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void reportsEveryCrossFieldErrorAtOnce() {
        TransporterInputRequest request = new TransporterInputRequest(
                List.of(lane(1), lane(1)),
                List.of(transporter(1, quote(1), quote(1)), transporter(1, quote(99))));

        InvalidInputException ex = catchThrowableOfType(InvalidInputException.class,
                () -> validator.validate(request));

        assertThat(ex.getErrors()).containsExactlyInAnyOrder(
                "lanes: duplicate lane id 1",
                "transporters: duplicate transporter id 1",
                "transporter 1: duplicate quote for lane 1",
                "transporter 1: lane 99 does not exist");
    }
}
