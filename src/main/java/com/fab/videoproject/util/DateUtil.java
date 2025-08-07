package com.fab.videoproject.util;

import java.time.LocalDateTime;

/**
 * Simple date utilities.
 */
public final class DateUtil {

    private DateUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}

