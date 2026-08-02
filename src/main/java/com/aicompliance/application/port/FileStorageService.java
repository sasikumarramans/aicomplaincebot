package com.aicompliance.application.port;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

public interface FileStorageService {

    /**
     * Uploads the given content under {@code key} and returns the storage key to persist
     * (callers should namespace keys, e.g. by companyId/certificateId, before calling this).
     */
    String upload(String key, InputStream content, long contentLength, String contentType);

    InputStream download(String key);

    URL generatePresignedDownloadUrl(String key, Duration expiry);

    void delete(String key);
}
