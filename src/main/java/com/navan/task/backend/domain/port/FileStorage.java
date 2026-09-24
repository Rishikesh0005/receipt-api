package com.navan.task.backend.domain.port;

import org.springframework.web.multipart.MultipartFile;

/**
 * Output port: File storage contract.
 */
public interface FileStorage {
    /**
     * Store an uploaded file.
     * @param file the multipart file
     * @param receiptId unique receipt identifier
     * @return file path where the file was stored
     */
    String store(MultipartFile file, String receiptId);
}
