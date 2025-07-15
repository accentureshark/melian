package org.shark.melian.test;

import org.shark.melian.util.PDFTestUtils;

import java.nio.file.Paths;

public class CreateTestPDFs {
    public static void main(String[] args) throws Exception {
        // Create test PDF files
        PDFTestUtils.createTestPDF(
            Paths.get("/tmp/melian-scan/matrix_review.pdf"),
            "The Matrix - Movie Review",
            "The Matrix is a groundbreaking science fiction film.\n" +
            "It explores themes of reality, simulation, and free will.\n" +
            "The movie was directed by the Wachowski sisters.\n" +
            "It features Neo, a computer programmer who discovers\n" +
            "that reality is actually a computer simulation."
        );
        
        PDFTestUtils.createTestPDF(
            Paths.get("/tmp/melian-scan/inception_analysis.pdf"),
            "Inception - Film Analysis",
            "Inception is a complex heist film about dreams.\n" +
            "The movie explores multiple layers of reality.\n" +
            "Dom Cobb is a skilled thief who specializes in\n" +
            "extraction of secrets from people's dreams.\n" +
            "The film was directed by Christopher Nolan."
        );
        
        PDFTestUtils.createTestPDF(
            Paths.get("/tmp/melian-scan/movie_guide.pdf"),
            "Movie Guide - PDF Document",
            "This is a general movie guide.\n" +
            "It contains information about various films.\n" +
            "You can use this to understand movie plots,\n" +
            "characters, and themes.\n" +
            "This document is for testing PDF parsing functionality."
        );
        
        System.out.println("Test PDF files created successfully!");
    }
}