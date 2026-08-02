package com.aicompliance.infrastructure.ocr;

import com.aicompliance.application.port.OcrTextExtractor;
import org.springframework.stereotype.Service;

@Service
public class CompositeOcrTextExtractor implements OcrTextExtractor {

    private static final String MIME_PDF = "application/pdf";
    private static final String MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final int RENDER_DPI = 300;

    private final PdfBoxTextExtractor pdfBoxTextExtractor;
    private final TesseractImageExtractor tesseractImageExtractor;
    private final DocxTextExtractor docxTextExtractor;

    public CompositeOcrTextExtractor(PdfBoxTextExtractor pdfBoxTextExtractor,
            TesseractImageExtractor tesseractImageExtractor, DocxTextExtractor docxTextExtractor) {
        this.pdfBoxTextExtractor = pdfBoxTextExtractor;
        this.tesseractImageExtractor = tesseractImageExtractor;
        this.docxTextExtractor = docxTextExtractor;
    }

    @Override
    public ExtractedText extractText(byte[] fileBytes, String mimeType) {
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType is required to select an OCR strategy");
        }

        if (mimeType.equals(MIME_PDF)) {
            return extractFromPdf(fileBytes);
        }
        if (mimeType.equals(MIME_DOCX)) {
            return new ExtractedText(docxTextExtractor.extractText(fileBytes), false);
        }
        if (mimeType.startsWith("image/")) {
            return new ExtractedText(tesseractImageExtractor.extractText(fileBytes), true);
        }

        throw new UnsupportedOcrMimeTypeException(mimeType);
    }

    private ExtractedText extractFromPdf(byte[] fileBytes) {
        String embeddedText = pdfBoxTextExtractor.extractEmbeddedText(fileBytes);
        if (!pdfBoxTextExtractor.isLikelyScanned(embeddedText)) {
            return new ExtractedText(embeddedText, false);
        }

        // No usable text layer - likely a scanned PDF. Render each page to an image and OCR it.
        byte[][] pageImages = pdfBoxTextExtractor.renderPagesAsImages(fileBytes, RENDER_DPI);
        StringBuilder ocrText = new StringBuilder();
        for (byte[] pageImage : pageImages) {
            ocrText.append(tesseractImageExtractor.extractText(pageImage)).append('\n');
        }
        return new ExtractedText(ocrText.toString(), true);
    }
}
