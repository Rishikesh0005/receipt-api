package com.navan.task.backend.infrastructure.adapter;

import com.navan.task.backend.domain.port.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Adapter: file storage using local disk.
 */
@Component
public class LocalFileStorage implements FileStorage {
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file, String receiptId) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            // Save file with receipt ID
            String fileName = receiptId + "_" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt");
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
