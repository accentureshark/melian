// src/test/java/org/shark/melian/McpServerRestAssuredIntegrationTest.java
package org.shark.melian;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("integration")
public class McpServerRestAssuredIntegrationTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 3000; // Puerto mapeado en docker-compose.yml
    }

    @Test
    void healthEndpointReturnsOk() {
        given()
            .get("/mcp/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("OK"))
            .body("tools", notNullValue())
            .body("resources", notNullValue());
    }

    @Test
    void initializeReturnsCapabilities() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":1}";
        given()
            .body(request)
            .header("Content-Type", "application/json")
        .when()
            .post("/mcp")
        .then()
            .statusCode(200)
            .body("result.serverInfo.name", equalTo("melian-movie-server"))
            .body("result.capabilities.tools", notNullValue())
            .body("result.capabilities.resources", notNullValue());
    }

    @Test
    void toolsListReturnsRegisteredTools() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":2}";
        given()
            .body(request)
            .header("Content-Type", "application/json")
        .when()
            .post("/mcp")
        .then()
            .statusCode(200)
            .body("result.tools", hasItems("search_movies", "get_movie_chunks", "get_server_status"));
    }

    @Test
    void callGetServerStatusTool() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"get_server_status\",\"arguments\":{}},\"id\":3}";
        given()
            .body(request)
            .header("Content-Type", "application/json")
        .when()
            .post("/mcp")
        .then()
            .statusCode(200)
            .body("result.status", equalTo("OK"));
    }

    @Test
    void resourcesListReturnsResources() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"resources/list\",\"id\":4}";
        given()
            .body(request)
            .header("Content-Type", "application/json")
        .when()
            .post("/mcp")
        .then()
            .statusCode(200)
            .body("result.resources", notNullValue());
    }

    @Test
    void resourcesReadReturnsErrorForInvalidUri() {
        String request = "{\"jsonrpc\":\"2.0\",\"method\":\"resources/read\",\"params\":{\"uri\":\"invalid-resource\"},\"id\":5}";
        given()
            .body(request)
            .header("Content-Type", "application/json")
        .when()
            .post("/mcp")
        .then()
            .statusCode(200)
            .body("error.code", equalTo(-32603));
    }
}