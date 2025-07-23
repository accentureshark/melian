import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.shark.melian.MelianMcpServer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("integration")
public class McpServerRestAssuredIntegrationTest {

    private static MelianMcpServer server;

    @BeforeAll
    static void startServer() throws Exception {
        server = new MelianMcpServer();
        var startMethod = MelianMcpServer.class.getDeclaredMethod("startHttpServer");
        startMethod.setAccessible(true);
        startMethod.invoke(server);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 3000;
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.shutdown();
            HttpServer httpServer = server.getHttpServer();
            if (httpServer != null) {
                httpServer.stop(0);
            }
        }
    }

    @Test
    void healthEndpointReturnsOk() {
        given()
            .get("/mcp/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("OK"))
            .body("tools", notNullValue());
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
            .body("result.capabilities.tools", notNullValue());
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
}
