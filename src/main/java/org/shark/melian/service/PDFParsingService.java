package org.shark.melian.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Service for parsing PDF files and extracting text content.
 */
@Service
public class PDFParsingService {
    
    private static final Logger log = Logger.getLogger(PDFParsingService.class.getName());
    
    public PDFParsingService() {
        log.info("[PDFParsingService] Initialized");
    }
    
    /**
     * Extract text content from a PDF file
     */
    public String extractTextFromPDF(Path pdfPath) {
        log.info("[PDFParsingService] Extracting text from PDF: " + pdfPath);
        
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            // Clean up the text a bit
            text = text.replaceAll("\\s+", " ").trim();
            
            log.info("[PDFParsingService] Extracted " + text.length() + " characters from PDF");
            return text;
            
        } catch (IOException e) {
            log.severe("[PDFParsingService] Error extracting text from PDF: " + pdfPath + " - " + e.getMessage());
            return "Error extracting text from PDF: " + e.getMessage();
        }
    }
    
    /**
     * Extract text content from a PDF file with page range
     */
    public String extractTextFromPDF(Path pdfPath, int startPage, int endPage) {
        log.info("[PDFParsingService] Extracting text from PDF: " + pdfPath + " (pages " + startPage + "-" + endPage + ")");
        
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPage);
            stripper.setEndPage(endPage);
            String text = stripper.getText(document);
            
            // Clean up the text a bit
            text = text.replaceAll("\\s+", " ").trim();
            
            log.info("[PDFParsingService] Extracted " + text.length() + " characters from PDF pages " + startPage + "-" + endPage);
            return text;
            
        } catch (IOException e) {
            log.severe("[PDFParsingService] Error extracting text from PDF: " + pdfPath + " - " + e.getMessage());
            return "Error extracting text from PDF: " + e.getMessage();
        }
    }
    
    /**
     * Get basic information about a PDF file
     */
    public String getPDFInfo(Path pdfPath) {
        log.info("[PDFParsingService] Getting PDF info: " + pdfPath);
        
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            int pageCount = document.getNumberOfPages();
            
            String info = String.format("PDF Info - Pages: %d, File: %s", pageCount, pdfPath.getFileName());
            
            log.info("[PDFParsingService] PDF has " + pageCount + " pages");
            return info;
            
        } catch (IOException e) {
            log.severe("[PDFParsingService] Error getting PDF info: " + pdfPath + " - " + e.getMessage());
            return "Error getting PDF info: " + e.getMessage();
        }
    }
}