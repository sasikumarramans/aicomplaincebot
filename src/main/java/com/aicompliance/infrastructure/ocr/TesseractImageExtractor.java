package com.aicompliance.infrastructure.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wraps Tess4J for OCR of raster images (JPG/PNG uploads, and PDF pages rendered to images by
 * {@link PdfBoxTextExtractor}). Requires the native Tesseract binary and trained-data files to
 * be present on the host/container - see docs/BACKEND_PLAN.md for the local dev + Docker setup
 * note.
 */
@Component
public class TesseractImageExtractor {

    private final Tesseract tesseract;

    public TesseractImageExtractor(@Value("${app.ocr.tessdata-path:}") String tessdataPath) {
        this.tesseract = new Tesseract();
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            tesseract.setDatapath(tessdataPath);
        }
        tesseract.setLanguage("eng");
    }

    public String extractText(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IllegalArgumentException("Unsupported or corrupt image data");
            }
            return tesseract.doOCR(image);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read image for OCR", e);
        } catch (TesseractException e) {
            throw new OcrProcessingException("Tesseract OCR failed", e);
        }
    }
}
