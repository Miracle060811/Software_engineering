package com.travelmate.common;

public final class PaginationSupport {

    private PaginationSupport() {
    }

    public static Page normalize(int page, int size, int maximumSize) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, maximumSize));
        long requestedOffset = (long) (safePage - 1) * safeSize;
        int safeOffset = requestedOffset > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) requestedOffset;
        return new Page(safePage, safeSize, safeOffset);
    }

    public static Window window(int page, int size, int maximumSize, int total) {
        Page normalized = normalize(page, size, maximumSize);
        int safeTotal = Math.max(total, 0);
        int start = Math.min(normalized.offset(), safeTotal);
        int end = (int) Math.min((long) start + normalized.size(), safeTotal);
        return new Window(normalized.page(), normalized.size(), start, end);
    }

    public record Page(int page, int size, int offset) {
    }

    public record Window(int page, int size, int start, int end) {
    }
}
