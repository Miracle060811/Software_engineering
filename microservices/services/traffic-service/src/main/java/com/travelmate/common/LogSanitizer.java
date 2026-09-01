package com.travelmate.common;

public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String singleLine(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
                .replace('\r', '_')
                .replace('\n', '_');
    }
}
