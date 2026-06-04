package com.example.video_convert_platform.config.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件路径校验器
 * 校验路径是否存在且可访问
 * 
 */
public class DirectoryPathValidator implements ConstraintValidator<ValidDirectoryPath, String> {

    private boolean createIfNotExists;

    @Override
    public void initialize(ValidDirectoryPath annotation) {
        this.createIfNotExists = annotation.createIfNotExists();
    }

    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        try {
            Path pathObj = Paths.get(path);
            
            if (Files.exists(pathObj)) {
                return Files.isDirectory(pathObj) && Files.isWritable(pathObj);
            }
            
            if (createIfNotExists) {
                Files.createDirectories(pathObj);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
