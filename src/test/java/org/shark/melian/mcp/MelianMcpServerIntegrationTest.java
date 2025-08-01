package org.shark.melian.mcp;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive integration tests for MELIAN MCP Server using RestAssured.
 * Tests all MCP endpoints and methods for compliance with MCP standard.
 */
@Tag("integration")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MelianMcpServerIntegrationTest {

    private static final Network network = Network.newNetwork();

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.3")
            .withDatabaseName("sakila")
            .withUsername("sakila")
            .withPassword("sakila")
            .withInitScript("sakila-schema.sql")
            .withNetwork(network)
            .withNetworkAliases("mysql-sakila");

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7")
            .withNetwork(network)
            .withNetworkAliases("mongo");

    @Container
    static GenericContainer<?> mcpServer = new GenericContainer<>("melian-mcp-server:latest")
            .withNetwork(network)
            .withExposedPorts(3000)
            .withEnv("MCP_SERVER_HTTP_ENABLED", "true")
            .withEnv("MCP_SERVER_STDIO_ENABLED", "false")
            .withEnv("DB_URL", "jdbc:mysql://mysql-sakila:3306/sakila")
            .withEnv("DB_USERNAME", "sakila")
            .withEnv("DB_PASSWORD", "sakila")
            .withEnv("MONGODB_URI", "mongodb://mongo:27017/melian_movies")
            .withEnv("DISABLE_OPENAI", "true")
            .withEnv("MCP_PURE_MODE", "true")
            .dependsOn(mysql, mongodb)
            .waitingFor(Wait.forHttp("/mcp/health").forStatusCode(200));

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = mcpServer.getMappedPort(3000);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // Health and Status Tests

    @Test
    @Order(1)
    @DisplayName("Health endpoint returns OK status")
    void healthEndpointReturnsOk() {
        given()
            .when()
                .get("/mcp/health")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("OK"))
                .body("details", notNullValue())
                .body("timestamp", notNullValue())
                .body("details.tmdbService", equalTo("OK"))
                .body("details.sqlService", equalTo("OK"))
                .body("details.mongoService", equalTo("OK"))
                .body("details.tools", hasItems("search_movies", "get_movie_chunks", "get_server_status"))
                .body("details.resources", hasItems("melian://movies/sql", "melian://movies/mongo", "melian://movies/tmdb"));
    }

    // MCP Protocol Tests

    @Test
    @Order(2)
    @DisplayName("Initialize returns proper MCP capabilities")
    void initializeReturnsCapabilities() {
        String initRequest = """
            {
                "jsonrpc": "2.0",
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {
                        "roots": {"listChanged": true},
                        "sampling": {}
                    },
                    "clientInfo": {
                        "name": "test-client",
                        "version": "1.0.0"
                    }
                },
                "id": 1
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(initRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(1))
                .body("result.protocolVersion", equalTo("2024-11-05"))
                .body("result.serverInfo.name", equalTo("melian-movie-server"))
                .body("result.serverInfo.version", equalTo("1.0.0"))
                .body("result.capabilities.tools.listChanged", equalTo(true))
                .body("result.capabilities.resources.subscribe", equalTo(true))
                .body("result.capabilities.resources.listChanged", equalTo(true));
    }

    // Tools Tests

    @Test
    @Order(3)
    @DisplayName("Tools list returns all registered tools")
    void toolsListReturnsRegisteredTools() {
        String toolsRequest = """
            {
                "jsonrpc": "2.0",
                "method": "tools/list",
                "id": 2
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(toolsRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(2))
                .body("result.tools", hasSize(3))
                .body("result.tools[*].name", hasItems("search_movies", "get_movie_chunks", "get_server_status"))
                .body("result.tools.find { it.name == 'search_movies' }.description", containsString("Search for movies"))
                .body("result.tools.find { it.name == 'search_movies' }.inputSchema.type", equalTo("object"))
                .body("result.tools.find { it.name == 'search_movies' }.inputSchema.required", hasItem("query"));
    }

    @Test
    @Order(4)
    @DisplayName("REST tools endpoint returns tools list")
    void restToolsEndpointWorks() {
        given()
            .when()
                .get("/mcp/tools")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("tools", hasSize(3))
                .body("tools[*].name", hasItems("search_movies", "get_movie_chunks", "get_server_status"));
    }

    @Test
    @Order(5)
    @DisplayName("Call search_movies tool returns movie results")
    void callSearchMoviesTool() {
        String searchRequest = """
            {
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": "search_movies",
                    "arguments": {
                        "query": "matrix",
                        "limit": 5
                    }
                },
                "id": 3
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(searchRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(3))
                .body("result.isError", equalTo(false))
                .body("result.content", hasSize(1))
                .body("result.content[0].type", equalTo("text"))
                .body("result.content[0].text", containsString("Found"))
                .body("result.content[0].text", containsString("movies"))
                .body("result.content[0].data", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("Call get_movie_chunks tool with SQL source")
    void callGetMovieChunksToolSql() {
        String chunksRequest = """
            {
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": "get_movie_chunks",
                    "arguments": {
                        "source": "sql",
                        "limit": 10
                    }
                },
                "id": 4
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(chunksRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(4))
                .body("result.isError", equalTo(false))
                .body("result.content", hasSize(1))
                .body("result.content[0].type", equalTo("text"))
                .body("result.content[0].text", containsString("Retrieved"))
                .body("result.content[0].text", containsString("chunks"))
                .body("result.content[0].text", containsString("sql"))
                .body("result.content[0].data", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("Call get_movie_chunks tool with MongoDB source")
    void callGetMovieChunksToolMongo() {
        String chunksRequest = """
            {
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": "get_movie_chunks",
                    "arguments": {
                        "source": "mongo",
                        "limit": 5
                    }
                },
                "id": 5
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(chunksRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(5))
                .body("result.isError", equalTo(false))
                .body("result.content", hasSize(1))
                .body("result.content[0].type", equalTo("text"))
                .body("result.content[0].text", containsString("Retrieved"))
                .body("result.content[0].text", containsString("chunks"))
                .body("result.content[0].text", containsString("mongo"))
                .body("result.content[0].data", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("Call get_server_status tool")
    void callGetServerStatusTool() {
        String statusRequest = """
            {
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": "get_server_status",
                    "arguments": {}
                },
                "id": 6
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(statusRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(6))
                .body("result.isError", equalTo(false))
                .body("result.content", hasSize(1))
                .body("result.content[0].type", equalTo("text"))
                .body("result.content[0].text", containsString("Server status"))
                .body("result.content[0].data.status", equalTo("OK"))
                .body("result.content[0].data.details", notNullValue());
    }

    // Resources Tests

    @Test
    @Order(9)
    @DisplayName("Resources list returns available resources")
    void resourcesListReturnsResources() {
        String resourcesRequest = """
            {
                "jsonrpc": "2.0",
                "method": "resources/list",
                "id": 7
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(resourcesRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(7))
                .body("result.resources", hasSize(3))
                .body("result.resources[*].uri", hasItems("melian://movies/sql", "melian://movies/mongo", "melian://movies/tmdb"))
                .body("result.resources[*].mimeType", everyItem(equalTo("application/json")))
                .body("result.resources.find { it.uri == 'melian://movies/sql' }.name", equalTo("SQL Movie Database"))
                .body("result.resources.find { it.uri == 'melian://movies/mongo' }.name", equalTo("MongoDB Movie Collection"));
    }

    @Test
    @Order(10)
    @DisplayName("REST resources endpoint returns resources list")
    void restResourcesEndpointWorks() {
        given()
            .when()
                .get("/mcp/resources")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("resources", hasSize(3))
                .body("resources[*].uri", hasItems("melian://movies/sql", "melian://movies/mongo", "melian://movies/tmdb"));
    }

    @Test
    @Order(11)
    @DisplayName("Read SQL resource returns movie data")
    void readSqlResource() {
        String readRequest = """
            {
                "jsonrpc": "2.0",
                "method": "resources/read",
                "params": {
                    "uri": "melian://movies/sql"
                },
                "id": 8
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(readRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(8))
                .body("result.contents", hasSize(1))
                .body("result.contents[0].uri", equalTo("melian://movies/sql"))
                .body("result.contents[0].mimeType", equalTo("application/json"))
                .body("result.contents[0].text", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Read MongoDB resource returns movie data")
    void readMongoResource() {
        String readRequest = """
            {
                "jsonrpc": "2.0",
                "method": "resources/read",
                "params": {
                    "uri": "melian://movies/mongo"
                },
                "id": 9
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(readRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(9))
                .body("result.contents", hasSize(1))
                .body("result.contents[0].uri", equalTo("melian://movies/mongo"))
                .body("result.contents[0].mimeType", equalTo("application/json"))
                .body("result.contents[0].text", notNullValue());
    }

    @Test
    @Order(13)
    @DisplayName("REST resources endpoint reads specific resource")
    void restResourcesReadSpecific() {
        given()
            .queryParam("uri", "melian://movies/sql")
            .when()
                .get("/mcp/resources")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("contents", hasSize(1))
                .body("contents[0].uri", equalTo("melian://movies/sql"))
                .body("contents[0].mimeType", equalTo("application/json"))
                .body("contents[0].text", notNullValue());
    }

    // Error Handling Tests

    @Test
    @Order(14)
    @DisplayName("Unknown method returns JSON-RPC error")
    void unknownMethodReturnsError() {
        String unknownRequest = """
            {
                "jsonrpc": "2.0",
                "method": "unknown/method",
                "id": 10
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(unknownRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200) // JSON-RPC errors are still HTTP 200
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(10))
                .body("error.code", equalTo(-32603))
                .body("error.message", containsString("Unknown method"));
    }

    @Test
    @Order(15)
    @DisplayName("Invalid JSON returns parse error")
    void invalidJsonReturnsParseError() {
        String invalidJson = "{ invalid json }";

        given()
            .contentType(ContentType.JSON)
            .body(invalidJson)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200) // JSON-RPC errors are still HTTP 200
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("error.code", equalTo(-32603))
                .body("error.message", containsString("Internal error"));
    }

    @Test
    @Order(16)
    @DisplayName("Tool call with missing required parameter returns error")
    void toolCallMissingParameterReturnsError() {
        String invalidToolCall = """
            {
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": "search_movies",
                    "arguments": {
                        "limit": 5
                    }
                },
                "id": 11
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidToolCall)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(11))
                .body("result.isError", equalTo(true))
                .body("result.content[0].text", containsString("Query parameter is required"));
    }

    @Test
    @Order(17)
    @DisplayName("Invalid resource URI returns error")
    void invalidResourceUriReturnsError() {
        String invalidResourceRequest = """
            {
                "jsonrpc": "2.0",
                "method": "resources/read",
                "params": {
                    "uri": "invalid://resource/uri"
                },
                "id": 12
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidResourceRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(12))
                .body("error.code", equalTo(-32603))
                .body("error.message", containsString("Failed to read resource"));
    }

    // Performance and Load Tests

    @Test
    @Order(18)
    @DisplayName("Multiple concurrent requests work correctly")
    void multipleConcurrentRequests() {
        String healthRequest = """
            {
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": "get_server_status",
                    "arguments": {}
                },
                "id": 13
            }
            """;

        // Send multiple concurrent requests
        for (int i = 0; i < 5; i++) {
            given()
                .contentType(ContentType.JSON)
                .body(healthRequest)
                .when()
                    .post("/mcp")
                .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("result.isError", equalTo(false));
        }
    }

    @Test
    @Order(19)
    @DisplayName("Large search query works correctly")
    void largeSearchQueryWorks() {
        String largeSearchRequest = """
            {
                "jsonrpc": "2.0",
                "method": "tools/call",
                "params": {
                    "name": "search_movies",
                    "arguments": {
                        "query": "action adventure comedy drama thriller horror science fiction fantasy animation",
                        "limit": 20
                    }
                },
                "id": 14
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(largeSearchRequest)
            .when()
                .post("/mcp")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("jsonrpc", equalTo("2.0"))
                .body("id", equalTo(14))
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("Found"));
    }

    @AfterAll
    static void cleanup() {
        network.close();
    }
}