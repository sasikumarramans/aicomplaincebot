package com.aicompliance.application.port;

import java.util.List;

public interface ArchiveExportService {

    record ArchiveEntry(String path, byte[] content) {
    }

    byte[] buildZip(List<ArchiveEntry> entries);
}
