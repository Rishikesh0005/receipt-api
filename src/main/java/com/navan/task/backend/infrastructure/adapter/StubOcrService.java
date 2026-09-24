package com.navan.task.backend.infrastructure.adapter;

import com.navan.task.backend.domain.port.OcrService;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Adapter: OCR service that reads fixture text files for stubbing.
 * In production, this would call an actual OCR vendor API.
 */
@Component
public class StubOcrService implements OcrService {

    @Override
    public String extractText(String filePath) {
        try {
            // Read file content
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            return content;
        } catch (IOException e) {
            return null;
        }
    }
}
