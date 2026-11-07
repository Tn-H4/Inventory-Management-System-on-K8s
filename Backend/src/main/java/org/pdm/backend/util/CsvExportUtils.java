package org.pdm.backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CsvExportUtils {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private CsvExportUtils() {}

    public static String isoDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(ISO) : "";
    }

    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
