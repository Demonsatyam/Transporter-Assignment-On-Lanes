package com.freightfox.transporter.controller;

import com.freightfox.transporter.repository.InputDatasetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransporterApiIntegrationTest {

    private static final String INPUT = "/api/v1/transporters/input";
    private static final String ASSIGNMENT = "/api/v1/transporters/assignment";

    /** PDF Test Case 1. */
    private static final String TEST_CASE_1 = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 20835}, {"laneId": 2, "quote": 10512}, {"laneId": 3, "quote": 22105}, {"laneId": 4, "quote": 42481}, {"laneId": 5, "quote": 19862}]},
                {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 1, "quote": 48844}, {"laneId": 2, "quote": 31326}, {"laneId": 3, "quote": 18640}, {"laneId": 4, "quote": 45828}, {"laneId": 5, "quote": 18297}]},
                {"id": 3, "name": "Transporter T3", "laneQuotes": [{"laneId": 1, "quote": 39020}, {"laneId": 2, "quote": 20648}, {"laneId": 3, "quote": 31438}, {"laneId": 4, "quote": 36447}, {"laneId": 5, "quote": 12789}]},
                {"id": 4, "name": "Transporter T4", "laneQuotes": [{"laneId": 1, "quote": 14400}, {"laneId": 2, "quote": 44514}, {"laneId": 3, "quote": 14316}, {"laneId": 4, "quote": 10678}, {"laneId": 5, "quote": 13032}]},
                {"id": 5, "name": "Transporter T5", "laneQuotes": [{"laneId": 1, "quote": 11601}, {"laneId": 2, "quote": 19760}, {"laneId": 3, "quote": 40870}, {"laneId": 4, "quote": 20635}, {"laneId": 5, "quote": 26421}]},
                {"id": 6, "name": "Transporter T6", "laneQuotes": [{"laneId": 1, "quote": 35095}, {"laneId": 2, "quote": 12494}, {"laneId": 3, "quote": 17808}, {"laneId": 4, "quote": 36210}, {"laneId": 5, "quote": 39444}]},
                {"id": 7, "name": "Transporter T7", "laneQuotes": [{"laneId": 1, "quote": 26070}, {"laneId": 2, "quote": 41098}, {"laneId": 3, "quote": 20932}, {"laneId": 4, "quote": 16897}, {"laneId": 5, "quote": 27938}]}
              ]
            }
            """;

    /** PDF Test Case 2. */
    private static final String TEST_CASE_2 = """
            {
              "lanes": [
                {"id": 1, "origin": "Chandigarh", "destination": "Shimla"},
                {"id": 2, "origin": "Agra", "destination": "Kanpur"},
                {"id": 3, "origin": "Varanasi", "destination": "Gorakhpur"},
                {"id": 4, "origin": "Amritsar", "destination": "Ludhiana"},
                {"id": 5, "origin": "Coimbatore", "destination": "Madurai"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter X", "laneQuotes": [{"laneId": 1, "quote": 1000}, {"laneId": 2, "quote": 1500}]},
                {"id": 2, "name": "Transporter Y", "laneQuotes": [{"laneId": 3, "quote": 2000}, {"laneId": 4, "quote": 1800}]},
                {"id": 3, "name": "Transporter Z", "laneQuotes": [{"laneId": 5, "quote": 1200}]},
                {"id": 4, "name": "Transporter W", "laneQuotes": [{"laneId": 1, "quote": 1100}, {"laneId": 4, "quote": 1750}]}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoSpyBean
    private InputDatasetRepository datasetRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE input_dataset CASCADE");
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long submit(String body) throws Exception {
        String response = postJson(INPUT, body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceAll(".*\"datasetId\":(\\d+).*", "$1"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    // ---------- API 1: input ----------

    @Test
    void submitPersistsWholeDataset() throws Exception {
        postJson(INPUT, TEST_CASE_1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Input data saved successfully."))
                .andExpect(jsonPath("$.datasetId").isNumber());

        assertThat(count("input_dataset")).isEqualTo(1);
        assertThat(count("lane")).isEqualTo(5);
        assertThat(count("transporter")).isEqualTo(7);
        assertThat(count("lane_quote")).isEqualTo(35);
        assertThat(jdbc.queryForObject("SELECT quote FROM lane_quote q JOIN lane l ON l.id = q.lane_id "
                + "JOIN transporter t ON t.id = q.transporter_id WHERE l.lane_ref = 4 AND t.transporter_ref = 4",
                java.math.BigDecimal.class)).isEqualByComparingTo("10678");
    }

    @Test
    void rejectsMissingAndInvalidFields() throws Exception {
        postJson(INPUT, """
                {"lanes": [{"id": 1, "origin": "", "destination": "Delhi"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 0}, {"laneId": 1, "quote": -5}, {"laneId": 1, "quote": 10.123}]},
                                  {"name": "T2", "laneQuotes": []}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("lanes[0].origin"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("laneQuotes[0].quote: must be greater than 0"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("laneQuotes[1].quote: must be greater than 0"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("laneQuotes[2].quote: numeric value out of bounds"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("transporters[1].id: must not be null"))));
        assertThat(count("input_dataset")).isZero();
    }

    @Test
    void rejectsEmptyCollections() throws Exception {
        postJson(INPUT, "{\"lanes\": [], \"transporters\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    void rejectsDuplicatesAndUnknownLaneReferences() throws Exception {
        postJson(INPUT, """
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 1, "origin": "C", "destination": "D"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 10}, {"laneId": 1, "quote": 20}]},
                                  {"id": 1, "name": "T1 again", "laneQuotes": [{"laneId": 42, "quote": 10}]}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", containsInAnyOrder(
                        "lanes: duplicate lane id 1",
                        "transporters: duplicate transporter id 1",
                        "transporter 1: duplicate quote for lane 1",
                        "transporter 1: lane 42 does not exist")));
        assertThat(count("input_dataset")).isZero();
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson(INPUT, "{\"lanes\": [")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body."));
    }

    @Test
    void failedSaveLeavesNoPartialData() throws Exception {
        doAnswer(invocation -> {
            entityManager.persist(invocation.getArgument(0));
            entityManager.flush(); // rows are now written inside the open transaction...
            assertThat(count("lane_quote")).isEqualTo(35);
            throw new IllegalStateException("simulated failure after partial write"); // ...which must roll back
        }).when(datasetRepository).save(any());

        postJson(INPUT, TEST_CASE_1)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));

        assertThat(count("input_dataset")).isZero();
        assertThat(count("lane")).isZero();
        assertThat(count("lane_quote")).isZero();
    }

    // ---------- API 2: assignment ----------

    @Test
    void returnsOptimalAssignmentForTestCase1() throws Exception {
        submit(TEST_CASE_1);
        postJson(ASSIGNMENT, "{\"maxTransporters\": 3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.totalCost").value(60139))
                .andExpect(jsonPath("$.assignments", hasSize(5)))
                .andExpect(jsonPath("$.assignments[0].laneId").value(1))
                .andExpect(jsonPath("$.assignments[0].transporterId").value(5))
                .andExpect(jsonPath("$.assignments[1].transporterId").value(1))
                .andExpect(jsonPath("$.assignments[2].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[3].transporterId").value(4))
                .andExpect(jsonPath("$.assignments[4].transporterId").value(4))
                .andExpect(jsonPath("$.selectedTransporters", containsInAnyOrder(1, 4, 5)));
    }

    @Test
    void infeasibleLimitReturns409() throws Exception {
        submit(TEST_CASE_2);
        postJson(ASSIGNMENT, "{\"maxTransporters\": 2}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message", containsString("at least 3 transporters")));
    }

    @Test
    void laneWithoutQuotesReturns409() throws Exception {
        submit("""
                {"lanes": [{"id": 1, "origin": "A", "destination": "B"}, {"id": 2, "origin": "C", "destination": "D"}],
                 "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 10}]},
                                  {"id": 2, "name": "No quotes", "laneQuotes": []}]}
                """);
        postJson(ASSIGNMENT, "{\"maxTransporters\": 2}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("lane(s) [2]")));
    }

    @Test
    void invalidMaxTransportersReturns400() throws Exception {
        postJson(ASSIGNMENT, "{\"maxTransporters\": 0}").andExpect(status().isBadRequest());
        postJson(ASSIGNMENT, "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]", containsString("maxTransporters")));
    }

    @Test
    void noDatasetReturns404() throws Exception {
        postJson(ASSIGNMENT, "{\"maxTransporters\": 3}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("No input data found")));
    }

    @Test
    void unknownDatasetIdReturns404() throws Exception {
        postJson(ASSIGNMENT, "{\"maxTransporters\": 3, \"datasetId\": 999999}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Dataset 999999 not found."));
    }

    @Test
    void datasetsAreIsolatedAndLatestIsDefault() throws Exception {
        long first = submit(TEST_CASE_1);
        long second = submit(TEST_CASE_2);

        postJson(ASSIGNMENT, "{\"maxTransporters\": 4}")
                .andExpect(jsonPath("$.totalCost").value(7450));
        postJson(ASSIGNMENT, "{\"maxTransporters\": 3, \"datasetId\": " + first + "}")
                .andExpect(jsonPath("$.totalCost").value(60139));
        postJson(ASSIGNMENT, "{\"maxTransporters\": 4, \"datasetId\": " + second + "}")
                .andExpect(jsonPath("$.totalCost").value(7450));
    }

    @Test
    void unknownRouteReturns404WithoutStackTrace() throws Exception {
        postJson("/api/v1/nope", "{}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
