# 🧪 Ejemplos de Uso del Servidor MCP de MELIAN

## ¿Cómo interactuar con el servidor MCP?

El servidor MCP de MELIAN utiliza el protocolo estándar MCP vía STDIO (entrada/salida estándar). Esto significa que se comunica a través de mensajes JSON intercambiados por STDIN/STDOUT.

## 📋 Prerequisitos

1. **Servidor ejecutándose**:
   ```bash
   ./run-mcp-server.sh
   ```
   
2. **Cliente MCP** (uno de estos):
   - Claude Desktop (con configuración MCP)
   - MCP Inspector (herramienta oficial)
   - Cliente MCP personalizado
   - Scripts que implementen el protocolo MCP

## 🔧 Configuración en Claude Desktop

### 1. Abrir configuración de Claude Desktop:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%/Claude/claude_desktop_config.json`

### 2. Agregar configuración para MELIAN:

```json
{
  "mcpServers": {
    "melian-movies": {
      "command": "java",
      "args": [
        "-jar", 
        "/ruta/completa/a/melian/target/melian-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TMDB_ACCESS_TOKEN": "tu_token_tmdb_aqui"
      }
    }
  }
}
```

### 3. Reiniciar Claude Desktop

## 🛠️ Herramientas MCP Disponibles

Una vez conectado, el servidor expone estas herramientas:

### 1. `search_movies`
Busca películas por título o criterios.

**Parámetros**:
- `query` (string): Término de búsqueda
- `limit` (number, opcional): Máximo de resultados (default: 10)

**Ejemplo de uso en Claude**:
```
Busca películas que contengan "Matrix" usando la herramienta search_movies
```

### 2. `get_movie_details`
Obtiene detalles completos de una película específica.

**Parámetros**:
- `id` (string): ID de la película
- `source` (string, opcional): "sql" o "mongo" (default: "sql")

**Ejemplo de uso en Claude**:
```
Obtén los detalles completos de la película con ID "12345" usando get_movie_details
```

### 3. `get_movie_chunks`
Obtiene chunks de datos de películas para sistemas RAG.

**Parámetros**:
- `source` (string, opcional): "sql" o "mongo" (default: "sql")
- `limit` (number, opcional): Máximo de chunks (default: 10)
- `filter` (string, opcional): Filtro de búsqueda

**Ejemplo de uso en Claude**:
```
Dame 5 chunks de datos de películas usando get_movie_chunks con límite 5
```

## 📊 Recursos MCP Disponibles

### 1. `movies/metadata`
Metadata del sistema de películas.

### 2. `movies/schema`
Esquema de la base de datos de películas.

### 3. `movies/chunks`
Chunks de contenido para RAG.

## 🧪 Pruebas con MCP Inspector

### 1. Instalar MCP Inspector:
```bash
npm install -g @mcp/inspector
```

### 2. Probar conexión:
```bash
mcp-inspector java -jar target/melian-0.1.0-SNAPSHOT.jar
```

### 3. Explorar herramientas disponibles en la interfaz web.

## 📝 Ejemplo de Interacción Manual (Para Desarrollo)

### 1. Mensaje de inicialización:
```json
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {
      "name": "test-client",
      "version": "1.0.0"
    }
  },
  "id": 1
}
```

### 2. Lista de herramientas:
```json
{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "params": {},
  "id": 2
}
```

### 3. Llamar herramienta search_movies:
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "search_movies",
    "arguments": {
      "query": "Matrix",
      "limit": 5
    }
  },
  "id": 3
}
```

## 🎯 Casos de Uso Comunes

### 1. **Búsqueda de Películas con IA**
```
"Busca películas de ciencia ficción de los años 80 y dame un resumen de las 3 más populares"
```

### 2. **Análisis de Datos Cinematográficos**
```
"Analiza las tendencias de calificaciones de películas por década usando los chunks disponibles"
```

### 3. **Recomendaciones Personalizadas**
```
"Basándote en la información de películas disponible, recomiéndame 5 películas similares a 'Blade Runner'"
```

### 4. **Investigación de Géneros**
```
"Dame estadísticas sobre el género de terror en el cine, incluyendo evolución temporal y directores destacados"
```

## ⚠️ Troubleshooting

### Error: "Server not responding"
1. Verificar que el servidor esté corriendo
2. Comprobar que no hay errores en los logs
3. Reiniciar el servidor

### Error: "Tool not found"
1. Verificar que el servidor MCP tenga las herramientas registradas
2. Comprobar la configuración del cliente MCP

### Error: "TMDB API issues"
1. Verificar que `TMDB_ACCESS_TOKEN` esté configurado
2. Comprobar que el token sea válido
3. Verificar conectividad a internet

## 📚 Recursos Adicionales

- [MCP Specification](https://modelcontextprotocol.io/docs/specification/)
- [Claude MCP Guide](https://docs.anthropic.com/claude/docs/mcp)
- [TMDB API Docs](https://developers.themoviedb.org/3)

## 💡 Tips para Desarrollo

1. **Logs detallados**: El servidor muestra logs detallados para debug
2. **Múltiples fuentes**: Puedes alternar entre SQL y MongoDB como fuente
3. **Configuración flexible**: Usa variables de entorno para diferentes entornos
4. **Extensibilidad**: El sistema está diseñado para agregar nuevas herramientas fácilmente