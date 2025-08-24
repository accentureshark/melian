package org.shark.melian.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
//import org.springframework.test.context.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = org.shark.melian.rest.McpController.class)
@Import(McpService.class)
// @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class McpControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void initializeReturnsResult() throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\"}}";
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"));
    }

    @Test
    void toolsListReturnsTools() throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}";
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools").isArray());
    }

    @Test
    void pingWithoutInitializeReturnsError() throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"ping\",\"params\":{}}";
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").isNumber());
    }

    @Test
    void pingAfterInitializeWorks() throws Exception {
        String init = "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"initialize\"}";
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(init))
                .andExpect(status().isOk());
        String ping = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"ping\",\"params\":{}}";
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(ping))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.ok").value(true));
    }

    @Test
    void unknownMethodReturnsNotFound() throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"does/not/exist\"}";
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601));
    }

    @Test
    void internalErrorWrappedAsJsonRpc() throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"raise_error\",\"arguments\":{}}}";
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32603));
    }
}

