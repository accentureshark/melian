package org.shark.melian.assistant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.shark.melian.tools.SimpleMovieTools;

/**
 * Simplified AI Assistant for basic tool execution and demonstration of streaming concepts
 */
@Component
@Slf4j
public class MelianAiAssistant {

    private final SimpleMovieTools movieTools;
    private final boolean aiEnabled;

    @Value("${OPENAI_API_KEY:#{null}}")
    private String openAiApiKey;

    public MelianAiAssistant(SimpleMovieTools movieTools) {
        this.movieTools = movieTools;
        this.aiEnabled = openAiApiKey != null && !openAiApiKey.isEmpty();
        
        if (aiEnabled) {
            log.info("🤖 AI Assistant initialized with OpenAI integration");
        } else {
            log.info("🔧 AI Assistant running in tool-only mode (no OpenAI API key)");
        }
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public boolean isStreamingEnabled() {
        return aiEnabled; // For demonstration, streaming is available when AI is enabled
    }

    /**
     * Simple chat that processes commands directly or simulates AI responses
     */
    public String chat(String sessionId, String message) {
        log.info("Processing chat message: {} (session: {})", message, sessionId);
        
        // Handle tool commands directly
        if (message.toLowerCase().startsWith("search ")) {
            String query = message.substring(7).trim();
            return movieTools.searchMovies(query, 10).toString();
        } else if (message.toLowerCase().equals("status")) {
            return movieTools.getServerStatus();
        } else if (message.toLowerCase().startsWith("chunks ")) {
            String params = message.substring(7).trim();
            return movieTools.getMovieChunks(10, params);
        }
        
        if (aiEnabled) {
            // Simulate AI response for demonstration
            return generateSimulatedResponse(message);
        } else {
            return "Available commands: 'search <query>', 'status', 'chunks <filter>'. Set OPENAI_API_KEY for AI chat.";
        }
    }

    /**
     * Simulate streaming response by breaking text into chunks
     */
    public void streamingChat(String sessionId, String message, StreamingHandler handler) {
        log.info("Processing streaming chat message: {} (session: {})", message, sessionId);
        
        try {
            handler.onStart();
            
            String response = chat(sessionId, message);
            
            // Simulate streaming by sending response in chunks
            String[] words = response.split(" ");
            for (int i = 0; i < words.length; i++) {
                String chunk = words[i] + (i < words.length - 1 ? " " : "");
                handler.onNext(chunk);
                
                // Simulate processing delay for demonstration
                Thread.sleep(100);
            }
            
            handler.onComplete();
        } catch (Exception e) {
            handler.onError(e);
        }
    }

    private String generateSimulatedResponse(String message) {
        if (message.toLowerCase().contains("movie") || message.toLowerCase().contains("film")) {
            return "I can help you search for movies using the TMDB database. You can ask me to search for specific movies, get movie information, or check the server status. For example, try 'search Matrix' or 'search comedy 2020'.";
        } else if (message.toLowerCase().contains("hello") || message.toLowerCase().contains("hi")) {
            return "Hello! I'm MELIAN, your movie information assistant. I can help you search for movies, get movie data chunks, and provide server status. What would you like to know about movies?";
        } else {
            return "I'm a movie information assistant powered by TMDB. I can search for movies, provide movie data, and check server status. Try asking me about movies or use commands like 'search <movie name>'.";
        }
    }

    public SimpleMovieTools getMovieTools() {
        return movieTools;
    }

    /**
     * Interface for handling streaming responses
     */
    public interface StreamingHandler {
        void onStart();
        void onNext(String chunk);
        void onComplete();
        void onError(Throwable error);
    }
}