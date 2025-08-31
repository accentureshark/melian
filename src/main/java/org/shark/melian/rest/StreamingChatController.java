package org.shark.melian.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shark.melian.assistant.MelianAiAssistant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * REST Controller that provides streaming AI chat responses using Server-Sent Events (SSE)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class StreamingChatController {

    private final MelianAiAssistant aiAssistant;

    /**
     * Regular (non-streaming) chat endpoint
     */
    @PostMapping("/message")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("Regular chat request: {}", request.getMessage());
        
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        String response = aiAssistant.chat(sessionId, request.getMessage());
        
        return new ChatResponse(response, false);
    }

    /**
     * Streaming chat endpoint using Server-Sent Events (SSE)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        log.info("Streaming chat request: {}", request.getMessage());

        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        
        if (!aiAssistant.isStreamingEnabled()) {
            try {
                emitter.send(SseEmitter.event()
                    .data("Streaming not available. Running in tool-only mode. Try commands like 'search Matrix' or 'status'.")
                    .name("error"));
                emitter.complete();
            } catch (IOException e) {
                log.error("Error sending SSE error message", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }

        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

        // Handle streaming response using simplified interface
        MelianAiAssistant.StreamingHandler handler = new MelianAiAssistant.StreamingHandler() {
            @Override
            public void onStart() {
                try {
                    emitter.send(SseEmitter.event()
                        .data("start")
                        .name("status"));
                } catch (IOException e) {
                    log.error("Error sending SSE start event", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onNext(String chunk) {
                try {
                    emitter.send(SseEmitter.event()
                        .data(chunk)
                        .name("token"));
                        
                    log.debug("Sent token: {}", chunk);
                } catch (IOException e) {
                    log.error("Error sending SSE token", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onComplete() {
                try {
                    emitter.send(SseEmitter.event()
                        .data("complete")
                        .name("status"));
                        
                    emitter.complete();
                    log.info("Streaming completed successfully");
                } catch (IOException e) {
                    log.error("Error completing SSE stream", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                try {
                    emitter.send(SseEmitter.event()
                        .data("Error: " + error.getMessage())
                        .name("error"));
                } catch (IOException e) {
                    log.error("Error sending SSE error", e);
                }
                emitter.completeWithError(error);
                log.error("Streaming error", error);
            }
        };

        // Start streaming chat in a separate thread
        new Thread(() -> {
            try {
                aiAssistant.streamingChat(sessionId, request.getMessage(), handler);
            } catch (Exception e) {
                handler.onError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * Get server status and streaming capabilities
     */
    @GetMapping("/status")
    public ChatStatusResponse getStatus() {
        return new ChatStatusResponse(
            aiAssistant.isAiEnabled(),
            aiAssistant.isStreamingEnabled(),
            "MELIAN AI Assistant"
        );
    }

    // Request/Response DTOs
    public static class ChatRequest {
        private String message;
        private String sessionId;

        public ChatRequest() {}

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    public static class ChatResponse {
        private String message;
        private boolean streaming;

        public ChatResponse(String message, boolean streaming) {
            this.message = message;
            this.streaming = streaming;
        }

        public String getMessage() {
            return message;
        }

        public boolean isStreaming() {
            return streaming;
        }
    }

    public static class ChatStatusResponse {
        private boolean aiEnabled;
        private boolean streamingEnabled;
        private String serverName;

        public ChatStatusResponse(boolean aiEnabled, boolean streamingEnabled, String serverName) {
            this.aiEnabled = aiEnabled;
            this.streamingEnabled = streamingEnabled;
            this.serverName = serverName;
        }

        public boolean isAiEnabled() {
            return aiEnabled;
        }

        public boolean isStreamingEnabled() {
            return streamingEnabled;
        }

        public String getServerName() {
            return serverName;
        }
    }
}