package com.fab.video_convert_platform.util;

import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for common digest algorithms.
 */
public final class DigestUtil {

    private DigestUtil() {
    }

    /**
     * Calculate MD5 hash of a file.
     *
     * @param path file path
     * @return md5 hex string
     * @throws IOException when read fails
     */
    public static String md5(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return DigestUtils.md5DigestAsHex(is);
        }
    }
}
