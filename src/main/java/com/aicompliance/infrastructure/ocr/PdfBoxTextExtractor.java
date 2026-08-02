package com.aicompliance.infrastructure.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extracts text from a PDF's embedded text layer. Scanned PDFs (image-only pages, no real text
 * layer) yield little or no text here - callers should treat a short/empty result as a signal to
 * fall back to image OCR rather than trusting an empty extraction as ground truth.
 */
@Component
public class PdfBoxTextExtractor {

    /** Below this character count, treat the PDF as effectively textless (likely scanned). */
    private static final int MIN_MEANINGFUL_TEXT_LENGTH = 20;

    public String extractEmbeddedText(byte[] pdfBytes) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read PDF for text extraction", e);
        }
    }

    public boolean isLikelyScanned(String extractedText) {
        return extractedText == null || extractedText.trim().length() < MIN_MEANINGFUL_TEXT_LENGTH;
    }

    public byte[][] renderPagesAsImages(byte[] pdfBytes, int dpi) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            byte[][] pages = new byte[document.getNumberOfPages()][];
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                pages[i] = baos.toByteArray();
            }
            return pages;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render PDF pages as images", e);
        }
    }
}
