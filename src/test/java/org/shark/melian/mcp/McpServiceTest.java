package org.shark.melian.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.shark.melian.service.AggregatedMovieService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpServiceTest {

    private McpService service;
    private ObjectMapper mapper = new ObjectMapper();
    private AggregatedMovieService aggregatedMovieService;

    @BeforeEach
    void setUp() {
        aggregatedMovieService = Mockito.mock(AggregatedMovieService.class);
        service = new McpService(aggregatedMovieService);
    }

    @Test
    void initializeReturnsCapabilities() throws Exception {
        Object result = service.dispatch("initialize", JsonNodeFactory.instance.objectNode());
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("2024-11-05", map.get("protocolVersion"));
        assertTrue(map.containsKey("capabilities"));
    }

    @Test
    void pingRequiresInitialize() {
        assertThrows(IllegalStateException.class, () -> service.dispatch("ping", JsonNodeFactory.instance.objectNode()));
    }

    @Test
    void toolsListContainsDummyTool() throws Exception {
        Object result = service.dispatch("tools/list", null);
        Map<?,?> map = (Map<?,?>) result;
        List<?> tools = (List<?>) map.get("tools");
        assertFalse(tools.isEmpty());
    }

    @Test
    void toolsCallReturnsContent() throws Exception {
        ObjectNode params = mapper.createObjectNode();
        params.put("name", "ask_data");
        ObjectNode args = params.putObject("arguments");
        args.put("question", "hi");
        Object result = service.dispatch("tools/call", params);
        Map<?,?> map = (Map<?,?>) result;
        assertNotNull(map.get("content"));
    }
}
