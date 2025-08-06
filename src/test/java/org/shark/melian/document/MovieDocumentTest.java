package org.shark.melian.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovieDocumentTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserializeRating() throws Exception {
        MovieDocument movie = new MovieDocument("Test", "Overview", "2024", 8.5);

        String json = mapper.writeValueAsString(movie);
        JsonNode node = mapper.readTree(json);
        assertTrue(node.get("rating").isNumber());
        assertEquals(8.5, node.get("rating").doubleValue());

        MovieDocument result = mapper.readValue(json, MovieDocument.class);
        assertEquals(8.5, result.getRating());
    }

    @Test
    void shouldHandleNullRating() throws Exception {
        MovieDocument movie = new MovieDocument("Test", "Overview", "2024", null);

        String json = mapper.writeValueAsString(movie);
        JsonNode node = mapper.readTree(json);
        assertTrue(node.get("rating").isNull());

        MovieDocument result = mapper.readValue(json, MovieDocument.class);
        assertNull(result.getRating());
    }
}
