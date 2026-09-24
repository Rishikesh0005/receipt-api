package com.navan.task.backend.domain.port;

/**
 * Input port: OCR contract (can be stubbed or call external API).
 */
public interface OcrService {
    /**
     * Extract text from a receipt file.
     * @param filePath path to the receipt file
     * @return extracted text or null if extraction fails
     */
    String extractText(String filePath);
}
