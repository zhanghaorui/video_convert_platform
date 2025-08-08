package com.fab.videoproject.infra;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Service handling NFS storage interactions.
 */
@Component
public class NfsService {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Save uploaded file to target path within NFS.
     *
     * @param file uploaded multipart file
     * @param target absolute target path
     * @throws IOException if file cannot be stored
     */
    public void saveFile(MultipartFile file, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Save a chunk to temporary directory.
     */
    public void saveChunk(MultipartFile file, Path dir, int chunk) throws IOException {
        Files.createDirectories(dir);
        Path target = dir.resolve(String.valueOf(chunk));
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Merge stored chunks into target file.
     */
    public void mergeChunks(Path dir, Path target, int totalChunks) throws IOException {
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = dir.resolve(String.valueOf(i));
                try (InputStream in = Files.newInputStream(chunkFile)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }
    }
}
