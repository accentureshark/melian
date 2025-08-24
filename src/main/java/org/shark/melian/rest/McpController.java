package org.shark.melian.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Value;
import org.shark.melian.mcp.McpDto;
import org.shark.melian.mcp.McpService;
import org.shark.melian.mcp.PureMcpServer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring MVC controller exposing MCP endpoints.
 */
@RestController
@RequestMapping("/mcp")

public class McpController {

    private final McpService mcpService;

    private boolean helpersEnabled;

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @Operation(summary = "Handle MCP JSON-RPC requests")
    @PostMapping
    public McpDto.JsonRpcResponse handle(@RequestBody JsonNode request) {
        log.info("[MCP] JSON-RPC request: {}", request);
        Object id = extractId(request.get("id"));
        McpDto.JsonRpcResponse.JsonRpcResponseBuilder builder = McpDto.JsonRpcResponse.builder()
                .jsonrpc("2.0")
                .id(id);

        String jsonrpc = request.path("jsonrpc").asText(null);
        if (!"2.0".equals(jsonrpc)) {
            builder.error(McpDto.JsonRpcError.builder().code(-32600).message("Invalid JSON-RPC version").build());
            return builder.build();
        }

        String method = request.path("method").asText(null);
        JsonNode params = request.get("params");

        try {
            Object result = mcpService.dispatch(method, params);
            builder.result(result);
        } catch (NoSuchMethodException e) {
            builder.error(McpDto.JsonRpcError.builder().code(-32601).message(e.getMessage()).build());
        } catch (IllegalArgumentException e) {
            builder.error(McpDto.JsonRpcError.builder().code(-32602).message(e.getMessage()).build());
        } catch (Exception e) {
            builder.error(McpDto.JsonRpcError.builder().code(-32603).message(e.getMessage()).build());
        }

        return builder.build();
    }

    private Object extractId(JsonNode idNode) {
        if (idNode == null || idNode.isNull()) {
            return null;
        }
        if (idNode.isIntegralNumber()) {
            return idNode.longValue();
        }
        if (idNode.isNumber()) {
            return idNode.numberValue();
        }
        return idNode.asText();
    }

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public Map<String, Object> health() {
        return mcpService.health();
    }

    @Operation(
            summary = "List available tools (MCP-compliant, detallado para NQL)",
            description = "Devuelve una lista detallada de herramientas MCP relacionadas con películas, chunks, metadatos, recursos y queries NQL. Cada herramienta incluye ejemplos realistas de entrada y salida, facilitando la integración y exploración desde clientes NQL y Swagger.",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lista de herramientas MCP",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                        mediaType = "application/json",
                        schema = @io.swagger.v3.oas.annotations.media.Schema(
                            example = """
{
  "tools": [
    {
      "name": "find_movie_by_title",
      "description": "Busca películas por título (soporta coincidencia parcial)",
      "inputSchema": {"type": "object", "properties": {"title": {"type": "string", "description": "Título de la película a buscar"}}, "required": ["title"]},
      "outputSchema": {"type": "object", "properties": {"movies": {"type": "array", "items": {"type": "object", "properties": {"id": {"type": "string"}, "title": {"type": "string"}, "year": {"type": "integer"}, "genre": {"type": "string"}}}}}, "required": ["movies"]},
      "example": {
        "name": "find_movie_by_title",
        "arguments": {"title": "Inception"},
        "result": {
          "movies": [
            {"id": "tt1375666", "title": "Inception", "year": 2010, "genre": "Sci-Fi"},
            {"id": "tt1790736", "title": "Inception: The Cobol Job", "year": 2010, "genre": "Animation"}
          ]
        }
      },
      "category": "peliculas",
      "tags": ["busqueda", "titulo", "peliculas"]
    },
    {
      "name": "get_movie_details",
      "description": "Obtiene los detalles completos de una película por su ID",
      "inputSchema": {"type": "object", "properties": {"movieId": {"type": "string", "description": "ID de la película"}}, "required": ["movieId"]},
      "outputSchema": {"type": "object", "properties": {"id": {"type": "string"}, "title": {"type": "string"}, "year": {"type": "integer"}, "genre": {"type": "string"}, "overview": {"type": "string"}, "rating": {"type": "number"}}}, "required": ["id", "title", "year", "genre", "overview", "rating"]},
      "example": {
        "name": "get_movie_details",
        "arguments": {"movieId": "tt1375666"},
        "result": {
          "id": "tt1375666",
          "title": "Inception",
          "year": 2010,
          "genre": "Sci-Fi",
          "overview": "A thief who steals corporate secrets through the use of dream-sharing technology...",
          "rating": 8.8
        }
      },
      "category": "peliculas",
      "tags": ["detalle", "pelicula", "id"]
    },
    {
      "name": "list_movies_by_genre",
      "description": "Lista películas filtradas por género",
      "inputSchema": {"type": "object", "properties": {"genre": {"type": "string", "description": "Género de las películas a listar"}}, "required": ["genre"]},
      "outputSchema": {"type": "object", "properties": {"movies": {"type": "array", "items": {"type": "object", "properties": {"id": {"type": "string"}, "title": {"type": "string"}, "year": {"type": "integer"}}}}}, "required": ["movies"]},
      "example": {
        "name": "list_movies_by_genre",
        "arguments": {"genre": "Drama"},
        "result": {
          "movies": [
            {"id": "tt0111161", "title": "The Shawshank Redemption", "year": 1994},
            {"id": "tt0108052", "title": "Schindler's List", "year": 1993}
          ]
        }
      },
      "category": "peliculas",
      "tags": ["genero", "peliculas", "filtro"]
    },
    {
      "name": "top_rated_movies",
      "description": "Devuelve las películas mejor valoradas",
      "inputSchema": {"type": "object", "properties": {"limit": {"type": "integer", "description": "Cantidad de películas a devolver", "default": 10}}, "required": ["limit"]},
      "outputSchema": {"type": "object", "properties": {"movies": {"type": "array", "items": {"type": "object", "properties": {"id": {"type": "string"}, "title": {"type": "string"}, "rating": {"type": "number"}}}}}, "required": ["movies"]},
      "example": {
        "name": "top_rated_movies",
        "arguments": {"limit": 3},
        "result": {
          "movies": [
            {"id": "tt0111161", "title": "The Shawshank Redemption", "rating": 9.3},
            {"id": "tt0068646", "title": "The Godfather", "rating": 9.2},
            {"id": "tt0071562", "title": "The Godfather: Part II", "rating": 9.0}
          ]
        }
      },
      "category": "peliculas",
      "tags": ["top", "rating", "peliculas"]
    },
    {
      "name": "list_chunks",
      "description": "Lista chunks de datos paginados y filtrados por NQL",
      "inputSchema": {"type": "object", "properties": {"page": {"type": "integer", "description": "Página de resultados", "default": 1}, "size": {"type": "integer", "description": "Tamaño de página", "default": 10}, "filter": {"type": "string", "description": "Filtro NQL opcional sobre los chunks"}}, "required": ["page", "size"]},
      "outputSchema": {"type": "object", "properties": {"chunks": {"type": "array", "items": {"type": "object", "properties": {"id": {"type": "string"}, "content": {"type": "string"}, "source": {"type": "string"}, "metadata": {"type": "object"}}}}, "total": {"type": "integer"}}, "required": ["chunks", "total"]},
      "example": {
        "name": "list_chunks",
        "arguments": {"page": 1, "size": 2, "filter": "year > 2010"},
        "result": {
          "chunks": [
            {"id": "chunk1", "content": "Inception (2010) - Sci-Fi", "source": "movies", "metadata": {"year": 2010}},
            {"id": "chunk2", "content": "Interstellar (2014) - Sci-Fi", "source": "movies", "metadata": {"year": 2014}}
          ],
          "total": 2
        }
      },
      "category": "chunks",
      "tags": ["chunks", "paginacion", "nql"]
    },
    {
      "name": "get_metadata",
      "description": "Obtiene metadatos de un recurso (columnas, tipos, PK)",
      "inputSchema": {"type": "object", "properties": {"resource": {"type": "string", "description": "Nombre o ID del recurso"}}, "required": ["resource"]},
      "outputSchema": {"type": "object", "properties": {"metadata": {"type": "object", "properties": {"columns": {"type": "array", "items": {"type": "string"}}, "types": {"type": "array", "items": {"type": "string"}}, "primaryKey": {"type": "string"}}}}, "required": ["metadata"]},
      "example": {
        "name": "get_metadata",
        "arguments": {"resource": "movies"},
        "result": {
          "metadata": {
            "columns": ["id", "title", "year", "genre", "rating"],
            "types": ["string", "string", "integer", "string", "number"],
            "primaryKey": "id"
          }
        }
      },
      "category": "metadata",
      "tags": ["metadata", "estructura", "recurso"]
    },
    {
      "name": "list_resources",
      "description": "Lista todos los recursos disponibles (tablas, colecciones, endpoints)",
      "inputSchema": {"type": "object", "properties": {}, "required": []},
      "outputSchema": {"type": "object", "properties": {"resources": {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "type": {"type": "string"}, "description": {"type": "string"}}}}}, "required": ["resources"]},
      "example": {
        "name": "list_resources",
        "arguments": {},
        "result": {
          "resources": [
            {"name": "movies", "type": "table", "description": "Películas disponibles"},
            {"name": "chunks", "type": "table", "description": "Chunks de datos"},
            {"name": "metadata", "type": "table", "description": "Metadatos de recursos"}
          ]
        }
      },
      "category": "recursos",
      "tags": ["recursos", "exploracion"]
    },
    {
      "name": "query_movies_nql",
      "description": "Ejecuta una consulta NQL sobre películas y devuelve los resultados estructurados",
      "inputSchema": {"type": "object", "properties": {"nql": {"type": "string", "description": "Consulta NQL sobre películas"}}, "required": ["nql"]},
      "outputSchema": {"type": "object", "properties": {"results": {"type": "array", "items": {"type": "object", "properties": {"id": {"type": "string"}, "title": {"type": "string"}, "year": {"type": "integer"}, "genre": {"type": "string"}, "rating": {"type": "number"}}}}}, "required": ["results"]},
      "example": {
        "name": "query_movies_nql",
        "arguments": {"nql": "SELECT title, year FROM movies WHERE rating > 8 AND genre = 'Drama'"},
        "result": {
          "results": [
            {"id": "tt0111161", "title": "The Shawshank Redemption", "year": 1994, "genre": "Drama", "rating": 9.3},
            {"id": "tt0108052", "title": "Schindler's List", "year": 1993, "genre": "Drama", "rating": 9.0}
          ]
        }
      },
      "category": "nql",
      "tags": ["nql", "query", "peliculas"]
    }
  ]
}
"""
                        )
                    )
                )
            }
        )
        @GetMapping("/tools")
        public Object tools() throws Exception {
            // helpersEnabled se fuerza a true para facilitar pruebas y exploración
            return mcpService.dispatch("tools/list", null);
        }


    @Operation(
        summary = "List resources (MCP-compliant)",
        description = "Lista todos los recursos disponibles según el estándar MCP. Soporta paginación y filtros básicos.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{" +
                        "\"offset\": 0, " +
                        "\"limit\": 2" +
                    "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lista de recursos MCP",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        example = "{" +
                            "\"resources\": [" +
                                "{\"name\": \"movies\", \"type\": \"table\", \"description\": \"Películas disponibles\"}," +
                                "{\"name\": \"chunks\", \"type\": \"table\", \"description\": \"Chunks de datos\"}" +
                            "]," +
                            "\"total\": 3" +
                        "}"
                    )
                )
            )
        }
    )
    @PostMapping("/resources/list")
    public Map<String, Object> listResourcesMcp(@RequestBody(required = false) JsonNode params) {
        log.info("[MCP] /resources/list params: {}", params);
        return mcpService.listResourcesMcp(params);
    }

    @Operation(
        summary = "Read resource data (MCP-compliant)",
        description = "Lee datos de un recurso específico según el estándar MCP. Soporta paginación y filtros básicos.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{" +
                        "\"resource\": \"movies\", " +
                        "\"offset\": 0, " +
                        "\"limit\": 2" +
                    "}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Datos del recurso MCP",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        example = "{" +
                            "\"data\": [" +
                                "{\"id\": \"tt1375666\", \"title\": \"Inception\", \"year\": 2010, \"genre\": \"Sci-Fi\", \"rating\": 8.8}," +
                                "{\"id\": \"tt0111161\", \"title\": \"The Shawshank Redemption\", \"year\": 1994, \"genre\": \"Drama\", \"rating\": 9.3}" +
                            "]," +
                            "\"total\": 3" +
                        "}"
                    )
                )
            )
        }
    )
    @PostMapping("/resources/read")
    public Map<String, Object> readResourceMcp(@RequestBody JsonNode params) {
        log.info("[MCP] /resources/read params: {}", params);
        return mcpService.readResourceMcp(params);
    }
}
