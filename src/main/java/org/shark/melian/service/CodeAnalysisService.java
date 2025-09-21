package org.shark.melian.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Service for analyzing Java code following best practices.
 * Analyzes code formatting, documentation, structure, and framework usage.
 */
@Service
@Slf4j
public class CodeAnalysisService {

    public Map<String, Object> analyzeCode(String directoryPath) {
        log.info("Analyzing code in directory: {}", directoryPath);
        
        Map<String, Object> analysis = new HashMap<>();
        Path path = Paths.get(directoryPath);
        
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            analysis.put("error", "Directory not found or is not a directory: " + directoryPath);
            return analysis;
        }
        
        try {
            // Analyze different aspects of the code
            analysis.put("codeFormatting", analyzeCodeFormatting(path));
            analysis.put("documentation", analyzeDocumentation(path));
            analysis.put("structure", analyzeModularStructure(path));
            analysis.put("frameworks", analyzeFrameworkUsage(path));
            analysis.put("summary", generateSummary(analysis));
            
            log.info("Code analysis completed for: {}", directoryPath);
            
        } catch (Exception e) {
            log.error("Error analyzing code: {}", e.getMessage(), e);
            analysis.put("error", "Error analyzing code: " + e.getMessage());
        }
        
        return analysis;
    }
    
    private Map<String, Object> analyzeCodeFormatting(Path directory) throws IOException {
        Map<String, Object> formatting = new HashMap<>();
        List<String> issues = new ArrayList<>();
        int totalFiles = 0;
        int filesWithIssues = 0;
        
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                totalFiles++;
                List<String> lines = Files.readAllLines(file);
                List<String> fileIssues = analyzeFileFormatting(file, lines);
                if (!fileIssues.isEmpty()) {
                    filesWithIssues++;
                    issues.addAll(fileIssues);
                }
            }
        }
        
        formatting.put("totalFiles", totalFiles);
        formatting.put("filesWithIssues", filesWithIssues);
        formatting.put("issues", issues);
        formatting.put("score", totalFiles > 0 ? (double)(totalFiles - filesWithIssues) / totalFiles * 100 : 100);
        
        return formatting;
    }
    
    private List<String> analyzeFileFormatting(Path file, List<String> lines) {
        List<String> issues = new ArrayList<>();
        String fileName = file.getFileName().toString();
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            
            // Check for trailing whitespace
            if (line.endsWith(" ") || line.endsWith("\t")) {
                issues.add(fileName + ":" + lineNumber + " - Trailing whitespace");
            }
            
            // Check for tabs instead of spaces
            if (line.contains("\t")) {
                issues.add(fileName + ":" + lineNumber + " - Using tabs instead of spaces");
            }
            
            // Check line length (recommended max 120 characters)
            if (line.length() > 120) {
                issues.add(fileName + ":" + lineNumber + " - Line too long (" + line.length() + " characters)");
            }
        }
        
        return issues;
    }
    
    private Map<String, Object> analyzeDocumentation(Path directory) throws IOException {
        Map<String, Object> documentation = new HashMap<>();
        int totalClasses = 0;
        int documentedClasses = 0;
        int totalMethods = 0;
        int documentedMethods = 0;
        List<String> undocumentedItems = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                String content = String.join("\n", lines);
                String fileName = file.getFileName().toString();
                
                // Count classes and their documentation
                if (content.contains("class ") || content.contains("interface ")) {
                    totalClasses++;
                    if (content.contains("/**") && content.indexOf("/**") < content.indexOf("class")) {
                        documentedClasses++;
                    } else {
                        undocumentedItems.add(fileName + " - Class lacks documentation");
                    }
                }
                
                // Count methods and their documentation
                long methodCount = 0;
                long documentedMethodCount = 0;
                String[] lineArray = lines.toArray(new String[0]);
                
                for (int i = 0; i < lineArray.length; i++) {
                    String line = lineArray[i].trim();
                    if (line.matches(".*\\b(public|private|protected)\\s+.*\\(.*\\).*\\{?")) {
                        methodCount++;
                        
                        // Check if there's documentation before this method
                        boolean hasDocumentation = false;
                        for (int j = i - 1; j >= 0; j--) {
                            String prevLine = lineArray[j].trim();
                            if (prevLine.isEmpty()) {
                                continue; // Skip empty lines
                            }
                            if (prevLine.contains("/**") || prevLine.contains("*/") || prevLine.startsWith("*")) {
                                hasDocumentation = true;
                                break;
                            }
                            if (prevLine.startsWith("@")) {
                                continue; // Skip annotations
                            }
                            break; // Found a non-documentation line
                        }
                        
                        if (hasDocumentation) {
                            documentedMethodCount++;
                        }
                    }
                }
                totalMethods += methodCount;
                documentedMethods += documentedMethodCount;
            }
        }
        
        documentation.put("totalClasses", totalClasses);
        documentation.put("documentedClasses", documentedClasses);
        documentation.put("totalMethods", totalMethods);
        documentation.put("documentedMethods", documentedMethods);
        documentation.put("undocumentedItems", undocumentedItems);
        documentation.put("classDocumentationScore", totalClasses > 0 ? (double)documentedClasses / totalClasses * 100 : 100);
        documentation.put("methodDocumentationScore", totalMethods > 0 ? (double)documentedMethods / totalMethods * 100 : 100);
        
        return documentation;
    }
    
    private Map<String, Object> analyzeModularStructure(Path directory) throws IOException {
        Map<String, Object> structure = new HashMap<>();
        Set<String> packages = new HashSet<>();
        Map<String, Integer> packageStructure = new HashMap<>();
        List<String> suggestions = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    if (line.trim().startsWith("package ")) {
                        String packageName = line.trim().substring(8).replace(";", "");
                        packages.add(packageName);
                        
                        // Analyze package structure
                        String[] parts = packageName.split("\\.");
                        if (parts.length > 3) {
                            String layer = parts[parts.length - 1];
                            packageStructure.put(layer, packageStructure.getOrDefault(layer, 0) + 1);
                        }
                    }
                }
            }
        }
        
        // Check for common architectural patterns
        boolean hasController = packageStructure.containsKey("rest") || packageStructure.containsKey("controller");
        boolean hasService = packageStructure.containsKey("service");
        boolean hasRepository = packageStructure.containsKey("repository");
        boolean hasModel = packageStructure.containsKey("model") || packageStructure.containsKey("entity");
        
        if (!hasController) suggestions.add("Consider adding a controller/rest layer for API endpoints");
        if (!hasService) suggestions.add("Consider adding a service layer for business logic");
        if (!hasRepository) suggestions.add("Consider adding a repository layer for data access");
        if (!hasModel) suggestions.add("Consider adding a model/entity layer for data structures");
        
        structure.put("totalPackages", packages.size());
        structure.put("packageStructure", packageStructure);
        structure.put("hasLayeredArchitecture", hasController && hasService && hasRepository && hasModel);
        structure.put("suggestions", suggestions);
        
        return structure;
    }
    
    private Map<String, Object> analyzeFrameworkUsage(Path directory) throws IOException {
        Map<String, Object> frameworks = new HashMap<>();
        Map<String, Integer> annotationUsage = new HashMap<>();
        List<String> frameworkSuggestions = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file);
                
                // Check Spring Boot annotations
                if (content.contains("@RestController")) annotationUsage.put("@RestController", annotationUsage.getOrDefault("@RestController", 0) + 1);
                if (content.contains("@Service")) annotationUsage.put("@Service", annotationUsage.getOrDefault("@Service", 0) + 1);
                if (content.contains("@Repository")) annotationUsage.put("@Repository", annotationUsage.getOrDefault("@Repository", 0) + 1);
                if (content.contains("@Component")) annotationUsage.put("@Component", annotationUsage.getOrDefault("@Component", 0) + 1);
                if (content.contains("@Autowired")) annotationUsage.put("@Autowired", annotationUsage.getOrDefault("@Autowired", 0) + 1);
                
                // Check Lombok annotations
                if (content.contains("@Data")) annotationUsage.put("@Data", annotationUsage.getOrDefault("@Data", 0) + 1);
                if (content.contains("@Builder")) annotationUsage.put("@Builder", annotationUsage.getOrDefault("@Builder", 0) + 1);
                if (content.contains("@RequiredArgsConstructor")) annotationUsage.put("@RequiredArgsConstructor", annotationUsage.getOrDefault("@RequiredArgsConstructor", 0) + 1);
                if (content.contains("@Slf4j")) annotationUsage.put("@Slf4j", annotationUsage.getOrDefault("@Slf4j", 0) + 1);
                
                // Check for potential improvements
                if (content.contains("@Autowired") && content.contains("private")) {
                    frameworkSuggestions.add("Consider using @RequiredArgsConstructor instead of @Autowired for dependency injection");
                }
            }
        }
        
        frameworks.put("annotationUsage", annotationUsage);
        frameworks.put("suggestions", frameworkSuggestions);
        frameworks.put("usesSpringBoot", annotationUsage.containsKey("@RestController") || annotationUsage.containsKey("@Service"));
        frameworks.put("usesLombok", annotationUsage.containsKey("@Data") || annotationUsage.containsKey("@Builder"));
        
        return frameworks;
    }
    
    private Map<String, Object> generateSummary(Map<String, Object> analysis) {
        Map<String, Object> summary = new HashMap<>();
        
        // Calculate overall score
        double formattingScore = 0;
        double documentationScore = 0;
        
        if (analysis.containsKey("codeFormatting")) {
            Map<String, Object> formatting = (Map<String, Object>) analysis.get("codeFormatting");
            formattingScore = (Double) formatting.getOrDefault("score", 0.0);
        }
        
        if (analysis.containsKey("documentation")) {
            Map<String, Object> documentation = (Map<String, Object>) analysis.get("documentation");
            double classScore = (Double) documentation.getOrDefault("classDocumentationScore", 0.0);
            double methodScore = (Double) documentation.getOrDefault("methodDocumentationScore", 0.0);
            documentationScore = (classScore + methodScore) / 2;
        }
        
        double overallScore = (formattingScore + documentationScore) / 2;
        
        summary.put("overallScore", Math.round(overallScore * 100.0) / 100.0);
        summary.put("formattingScore", Math.round(formattingScore * 100.0) / 100.0);
        summary.put("documentationScore", Math.round(documentationScore * 100.0) / 100.0);
        
        List<String> recommendations = new ArrayList<>();
        if (formattingScore < 80) {
            recommendations.add("Improve code formatting and style consistency");
        }
        if (documentationScore < 70) {
            recommendations.add("Add more comprehensive documentation to classes and methods");
        }
        
        summary.put("recommendations", recommendations);
        summary.put("analysisTimestamp", new Date().toString());
        
        return summary;
    }
}