package com.fab.video_convert_platform.infra;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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
     * Copy an existing file into the archive.
     *
     * @param source existing file path
     * @param target target path within NFS
     * @throws IOException if copy fails
     */
    public void copyFile(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
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

    /**
     * Recursively delete a directory and all of its contents.
     *
     * @param dir directory to remove
     * @throws IOException if deletion fails
     */
    public void deleteRecursively(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
