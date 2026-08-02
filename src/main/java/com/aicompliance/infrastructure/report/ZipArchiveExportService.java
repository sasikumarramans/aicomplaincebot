package com.aicompliance.infrastructure.report;

import com.aicompliance.application.port.ArchiveExportService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ZipArchiveExportService implements ArchiveExportService {

    @Override
    public byte[] buildZip(List<ArchiveEntry> entries) {
        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                ZipOutputStream zipOutput = new ZipOutputStream(byteOutput)) {
            for (ArchiveEntry entry : entries) {
                zipOutput.putNextEntry(new ZipEntry(entry.path()));
                zipOutput.write(entry.content());
                zipOutput.closeEntry();
            }
            zipOutput.finish();
            return byteOutput.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build audit folder zip", e);
        }
    }
}
