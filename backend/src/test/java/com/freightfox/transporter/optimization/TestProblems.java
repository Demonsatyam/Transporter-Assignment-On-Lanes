package com.freightfox.transporter.optimization;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

/** Problem builders shared by optimizer tests, including the datasets from the assignment PDF. */
final class TestProblems {

    private TestProblems() {
    }

    static Quote q(long lane, long transporter, long cost) {
        return new Quote(lane, transporter, BigDecimal.valueOf(cost));
    }

    static List<Long> lanes(int count) {
        return LongStream.rangeClosed(1, count).boxed().toList();
    }

    /** Builds quotes from a matrix: row = transporter (id = row+1), column = lane (id = col+1). */
    static List<Quote> matrix(long[][] costs) {
        List<Quote> quotes = new ArrayList<>();
        for (int t = 0; t < costs.length; t++) {
            for (int l = 0; l < costs[t].length; l++) {
                quotes.add(q(l + 1, t + 1, costs[t][l]));
            }
        }
        return quotes;
    }

    /** PDF API example: 10 lanes, 7 transporters, each transporter quotes 2 lanes. */
    static List<Quote> pdfExampleQuotes() {
        return List.of(
                q(1, 1, 5000), q(2, 1, 6000),
                q(3, 2, 7000), q(4, 2, 4000),
                q(5, 3, 3000), q(6, 3, 2000),
                q(7, 4, 8000), q(8, 4, 5000),
                q(9, 5, 4500), q(10, 5, 3500),
                q(1, 6, 5500), q(2, 6, 6200),
                q(3, 7, 7500), q(4, 7, 4500));
    }

    /** PDF Test Case 1: 5 lanes, 7 transporters, full quote matrix. */
    static List<Quote> testCase1Quotes() {
        return matrix(new long[][]{
                {20835, 10512, 22105, 42481, 19862},
                {48844, 31326, 18640, 45828, 18297},
                {39020, 20648, 31438, 36447, 12789},
                {14400, 44514, 14316, 10678, 13032},
                {11601, 19760, 40870, 20635, 26421},
                {35095, 12494, 17808, 36210, 39444},
                {26070, 41098, 20932, 16897, 27938}});
    }

    /** PDF Test Case 2: 5 lanes, 4 transporters (X, Y, Z, W). */
    static List<Quote> testCase2Quotes() {
        return List.of(
                q(1, 1, 1000), q(2, 1, 1500),
                q(3, 2, 2000), q(4, 2, 1800),
                q(5, 3, 1200),
                q(1, 4, 1100), q(4, 4, 1750));
    }
}
