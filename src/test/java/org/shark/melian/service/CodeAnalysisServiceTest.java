package org.shark.melian.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CodeAnalysisServiceTest {

    private CodeAnalysisService codeAnalysisService;

    @BeforeEach
    void setUp() {
        codeAnalysisService = new CodeAnalysisService();
    }

    @Test
    void testAnalyzeNonExistentDirectory() {
        Map<String, Object> result = codeAnalysisService.analyzeCode("/non/existent/path");
        
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertEquals("Directory not found or is not a directory: /non/existent/path", result.get("error"));
    }

    @Test
    void testAnalyzeEmptyDirectory(@TempDir Path tempDir) {
        Map<String, Object> result = codeAnalysisService.analyzeCode(tempDir.toString());
        
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
        assertTrue(result.containsKey("codeFormatting"));
        assertTrue(result.containsKey("documentation"));
        assertTrue(result.containsKey("structure"));
        assertTrue(result.containsKey("frameworks"));
        assertTrue(result.containsKey("summary"));
        
        Map<String, Object> formatting = (Map<String, Object>) result.get("codeFormatting");
        assertEquals(0, formatting.get("totalFiles"));
        assertEquals(100.0, formatting.get("score"));
    }

    @Test
    void testAnalyzeJavaCodeWithIssues(@TempDir Path tempDir) throws IOException {
        // Create a test Java file with formatting issues
        Path javaFile = tempDir.resolve("TestClass.java");
        String content = "package com.test;\n" +
                        "\n" +
                        "import org.springframework.stereotype.Service;\n" +
                        "\n" +
                        "@Service\n" +
                        "public class TestClass {\n" +
                        "    private String name;    \n" +  // trailing whitespace
                        "    \n" +
                        "    public String getName() {\n" +
                        "        return name;\n" +
                        "    }\n" +
                        "    \n" +
                        "    public void setName(String name) {\n" +  // no documentation
                        "        this.name = name;\n" +
                        "    }\n" +
                        "}\n";
        Files.writeString(javaFile, content);
        
        Map<String, Object> result = codeAnalysisService.analyzeCode(tempDir.toString());
        
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
        
        Map<String, Object> formatting = (Map<String, Object>) result.get("codeFormatting");
        assertEquals(1, formatting.get("totalFiles"));
        assertEquals(1, formatting.get("filesWithIssues"));
        assertTrue(((Double) formatting.get("score")) < 100.0);
        
        Map<String, Object> documentation = (Map<String, Object>) result.get("documentation");
        assertEquals(1, documentation.get("totalClasses"));
        assertEquals(0, documentation.get("documentedClasses"));
        
        Map<String, Object> frameworks = (Map<String, Object>) result.get("frameworks");
        assertTrue((Boolean) frameworks.get("usesSpringBoot"));
        
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertTrue(summary.containsKey("overallScore"));
        assertTrue(summary.containsKey("recommendations"));
    }

    @Test
    void testAnalyzeWellFormattedCode(@TempDir Path tempDir) throws IOException {
        // Create well-formatted and documented Java files
        Path javaFile1 = tempDir.resolve("WellDocumentedService.java");
        String content1 = "package com.test.service;\n" +
                         "\n" +
                         "import org.springframework.stereotype.Service;\n" +
                         "\n" +
                         "/**\n" +
                         " * A well-documented service class.\n" +
                         " */\n" +
                         "@Service\n" +
                         "public class WellDocumentedService {\n" +
                         "    \n" +
                         "    private String data;\n" +
                         "    \n" +
                         "    /**\n" +
                         "     * Gets the data.\n" +
                         "     * @return the data\n" +
                         "     */\n" +
                         "    public String getData() {\n" +
                         "        return data;\n" +
                         "    }\n" +
                         "    \n" +
                         "    /**\n" +
                         "     * Sets the data.\n" +
                         "     * @param data the data to set\n" +
                         "     */\n" +
                         "    public void setData(String data) {\n" +
                         "        this.data = data;\n" +
                         "    }\n" +
                         "}\n";
        Files.writeString(javaFile1, content1);
        
        Path javaFile2 = tempDir.resolve("TestController.java");
        String content2 = "package com.test.rest;\n" +
                         "\n" +
                         "import org.springframework.web.bind.annotation.RestController;\n" +
                         "\n" +
                         "/**\n" +
                         " * Test REST controller.\n" +
                         " */\n" +
                         "@RestController\n" +
                         "public class TestController {\n" +
                         "}\n";
        Files.writeString(javaFile2, content2);
        
        Map<String, Object> result = codeAnalysisService.analyzeCode(tempDir.toString());
        
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
        
        Map<String, Object> formatting = (Map<String, Object>) result.get("codeFormatting");
        assertEquals(2, formatting.get("totalFiles"));
        assertEquals(0, formatting.get("filesWithIssues"));
        assertEquals(100.0, formatting.get("score"));
        
        Map<String, Object> documentation = (Map<String, Object>) result.get("documentation");
        assertEquals(2, documentation.get("totalClasses"));
        assertEquals(2, documentation.get("documentedClasses"));
        assertEquals(100.0, documentation.get("classDocumentationScore"));
        
        Map<String, Object> structure = (Map<String, Object>) result.get("structure");
        assertTrue(structure.containsKey("packageStructure"));
        
        Map<String, Object> frameworks = (Map<String, Object>) result.get("frameworks");
        assertTrue((Boolean) frameworks.get("usesSpringBoot"));
        
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(100.0, summary.get("overallScore"));
    }
}