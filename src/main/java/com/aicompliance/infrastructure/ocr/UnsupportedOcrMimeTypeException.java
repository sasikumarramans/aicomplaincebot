package com.aicompliance.infrastructure.ocr;

public class UnsupportedOcrMimeTypeException extends RuntimeException {

    public UnsupportedOcrMimeTypeException(String mimeType) {
        super("Unsupported file type for OCR/text extraction: " + mimeType);
    }
}
