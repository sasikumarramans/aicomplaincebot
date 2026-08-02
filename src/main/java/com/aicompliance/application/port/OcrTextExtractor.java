package com.aicompliance.application.port;

public interface OcrTextExtractor {

    /**
     * Extracts raw text from a document's bytes. Dispatch by MIME type is the implementation's
     * responsibility (PDF with an embedded text layer, scanned PDF, JPG/PNG, DOCX, ...).
     */
    ExtractedText extractText(byte[] fileBytes, String mimeType);

    record ExtractedText(String text, boolean usedOcrFallback) {
    }
}
