# Cómo ejecutar el servidor MCP de MELIAN

Este documento explica cómo ejecutar el servidor MCP (Model Content Protocol) de MELIAN después del refactor que lo convirtió en un servidor MCP compliant usando Spring AI.

## ¿Qué es este servidor MCP?

MELIAN es ahora un servidor MCP que se conecta vía STDIO (estándar de entrada/salida) y expone herramientas para consultar información de películas desde múltiples fuentes:
- Base de datos MySQL (Sakila)
- Base de datos MongoDB (MovieLens)  
- API REST externa (TMDB - The Movie Database)

## Prerrequisitos

### 1. Software requerido:
- **Java 17+** (verificar con `java -version`)
- **Maven 3.6+** (verificar con `mvn -version`)
- **Docker** (para bases de datos, verificar con `docker --version`)

### 2. Bases de datos opcionales:
- **MySQL con base Sakila** (puerto 3307)
- **MongoDB con datos MovieLens** (puerto 27017)

## Pasos para ejecutar el servidor MCP

### Opción 1: Ejecución básica (solo API REST)

Esta es la forma más rápida para probar el servidor MCP sin depender de bases de datos:

```bash
# 1. Compilar el proyecto
mvn clean compile

# 2. Ejecutar el servidor MCP
mvn spring-boot:run -Dspring.profiles.active=default
```

El servidor se ejecutará y estará listo para recibir comandos MCP vía STDIO.

### Opción 2: Ejecución completa (con todas las fuentes de datos)

Para utilizar todas las funcionalidades del servidor MCP:

```bash
# 1. Iniciar las bases de datos con Docker
docker-compose up -d

# 2. Esperar a que las bases de datos estén listas
sleep 30

# 3. Verificar que las bases de datos están corriendo
docker ps

# 4. Ejecutar el servidor MCP
mvn spring-boot:run -Dspring.profiles.active=default
```

### Opción 3: Usando variables de entorno

Si tienes un archivo `.env` con configuraciones específicas:

```bash
# Ejecutar con variables de entorno personalizadas
env $(cat .env | xargs) mvn spring-boot:run
```

## ¿Cómo funciona un servidor MCP?

### Comunicación STDIO
El servidor MCP de MELIAN se comunica mediante STDIO (stdin/stdout), que es el estándar para servidores MCP:

- **Entrada (stdin)**: Recibe comandos JSON-RPC del cliente MCP
- **Salida (stdout)**: Envía respuestas JSON-RPC al cliente MCP

### Herramientas disponibles

El servidor expone las siguientes herramientas MCP:

1. **search_movies_by_title**: Buscar películas por título
2. **search_movies_by_genre**: Buscar películas por género  
3. **get_movie_details**: Obtener detalles de una película específica
4. **get_popular_movies**: Obtener películas populares
5. **search_movies_by_actor**: Buscar películas por actor

## Conectarse al servidor MCP

### Ejemplo con cliente MCP genérico:

```bash
# El servidor debe estar corriendo en modo STDIO
# Ejemplo de comando JSON-RPC para listar herramientas:
echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}' | mvn spring-boot:run
```

### Ejemplo con Claude Desktop o cliente compatible:

1. **Configurar en claude_desktop_config.json**:
```json
{
  "mcpServers": {
    "melian": {
      "command": "mvn",
      "args": ["spring-boot:run"],
      "cwd": "/ruta/al/proyecto/melian"
    }
  }
}
```

2. **Reiniciar Claude Desktop** para que detecte el nuevo servidor MCP

## Verificar que el servidor funciona

### 1. Compilar y probar:
```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Ejecutar tests de integración específicos del MCP
mvn test -Dgroups=integration -Dtest=McpServerIntegrationTest
```

### 2. Probar conexión STDIO:

```bash
# Crear un script de prueba simple
echo '#!/bin/bash
echo "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"tools/list\", \"params\": {}}"
sleep 2
' > test_mcp.sh

chmod +x test_mcp.sh

# Probar el servidor MCP
./test_mcp.sh | mvn spring-boot:run
```

## Configuración

### Variables de entorno importantes:

- `TMDB_ACCESS_TOKEN`: Token para acceso a TMDB API
- `SPRING_DATASOURCE_URL`: URL de conexión a MySQL
- `SPRING_DATA_MONGODB_HOST`: Host de MongoDB

### Configuración en application.yaml:

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        stdio: true
        type: SYNC
        name: melian-movie-server
        version: 1.0.0
```

## Resolución de problemas

### Error de Java version:
```bash
# Verificar versión de Java
java -version
# Debe ser Java 17 o superior
```

### Error de conexión a base de datos:
```bash
# Verificar que Docker containers están corriendo
docker ps

# Verificar logs de contenedores
docker logs melian_mysql_1
docker logs melian_mongo_1
```

### Error de compilación:
```bash
# Limpiar y recompilar
mvn clean compile

# Si persiste, verificar dependencias
mvn dependency:resolve
```

## Documentación adicional

- **README.md**: Información general del proyecto
- **MCP.md**: Explicación detallada del protocolo MCP
- **INTEGRATION_TESTS.md**: Guía de tests de integración
- **MOVIE_SERVICES.md**: Documentación de servicios de películas

## Nota importante

Este servidor MCP está diseñado para ser usado por clientes MCP compatibles (como Claude Desktop, VS Code con extensiones MCP, etc.). No es un servidor web tradicional con endpoints HTTP.

La comunicación se realiza exclusivamente mediante JSON-RPC sobre STDIO, siguiendo la especificación del protocolo MCP.