package com.pratosh.orchestration.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvCellTest {

    @Test
    void quotesAndEscapes() {
        assertEquals("\"a\"\"b\"", AdminReportController.cell("a\"b"));
        assertEquals("", AdminReportController.cell(null));
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        assertEquals("\"'=SUM(A1)\"", AdminReportController.cell("=SUM(A1)"));
        assertEquals("\"'@cmd\"", AdminReportController.cell("@cmd"));
    }
}
