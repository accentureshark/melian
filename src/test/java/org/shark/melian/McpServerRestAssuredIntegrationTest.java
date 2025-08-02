// src/test/java/org/shark/melian/McpServerRestAssuredIntegrationTest.java
package org.shark.melian;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class McpServerRestAssuredIntegrationTest {

    @Test
    void toolsListReturnsRegisteredTools() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":2}";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/mcp")
                .then()
                .statusCode(200)
                .body("result.tools[*].name", hasItems("search_movies", "get_movie_chunks", "get_server_status"));
    }

    @Test
    void callGetServerStatusTool() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"get_server_status\",\"arguments\":{}},\"id\":3}";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/mcp")
                .then()
                .statusCode(200)
                .body("result.content[0].data.status", equalTo("OK"));
    }

    @Test
    void resourcesListReturnsResources() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"resources/list\",\"id\":4}";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/mcp")
                .then()
                .statusCode(200)
                .body("result.resources", notNullValue());
    }

    @Test
    void initializeReturnsCapabilities() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0.0\"}},\"id\":5}";
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/mcp")
                .then()
                .statusCode(200)
                .body("result.serverInfo.name", equalTo("melian-movie-server"));
    }

    @Test
    void healthEndpointReturnsOk() {
        given()
                .when()
                .get("/mcp/health")
                .then()
                .statusCode(200)
                .body("details.tools", notNullValue())
                .body("details.resources", notNullValue());
    }
}