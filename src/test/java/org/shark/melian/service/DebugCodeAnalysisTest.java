package org.shark.melian.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class DebugCodeAnalysisTest {

    @Test
    void debugAnalyzeCode() {
        CodeAnalysisService service = new CodeAnalysisService();
        Map<String, Object> result = service.analyzeCode("/tmp/debug-java");
        
        System.out.println("Result: " + result);
        
        Map<String, Object> formatting = (Map<String, Object>) result.get("codeFormatting");
        System.out.println("Formatting: " + formatting);
        
        Map<String, Object> documentation = (Map<String, Object>) result.get("documentation");
        System.out.println("Documentation: " + documentation);
    }
}