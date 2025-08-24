# MELIAN

> **Módulo de Embedding y Lógica Inteligente para Acceso Natural**

---
<p align="center">
  <img src="./docs/images/melian-logo.png" alt="melian-logo" width="160"/>
</p>
---

## ¿Qué es MELIAN?

**MELIAN** es una implementación de un servidor MCP (Model Content Protocol) multi-backend, capaz de acceder y federar múltiples repositorios de películas y datos relacionados (SQL, MongoDB, TMDB, archivos, etc.).

No es solo un software: es un *personaje* que entiende, transforma y sirve conocimiento a cualquier dominio de tu organización.

MELIAN se implementa como un **MCP Server** (Model Content Protocol), capaz de conectarse a bases de datos, archivos, APIs, documentos, planillas y sistemas legacy,  
y exponer chunks enriquecidos, embeddings y metadata lista para potenciar aplicaciones RAG y clientes de IA como [EvolvAI](https://github.com/tu-org/evolvai).

## MCP compliance

Melian expone un único endpoint **POST `/mcp`** que implementa [JSON-RPC 2.0](https://www.jsonrpc.org/specification) sobre HTTP o stdio, siguiendo el [estándar oficial Model Content Protocol](https://modelcontextprotocol.io/specification/2025-03-26). Todas las respuestas utilizan **HTTP 200**; los errores se entregan dentro del cuerpo como objeto `error` de JSON-RPC.

> ⚠️ **Nota:** Melian **no expone endpoints RESTful** como `GET /mcp/metadata` o `GET /mcp/chunks`, ya que estos no son parte obligatoria del estándar MCP. El transporte y la interfaz recomendada por MCP es JSON-RPC 2.0 sobre HTTP o stdio.

> ⚠️ **Nota:** Swagger/OpenAPI **no está disponible** porque la interfaz MCP es binaria/JSON-RPC y no RESTful.

### Métodos MCP soportados

- `initialize` – Negocia la versión del protocolo y devuelve capacidades del servidor.
- `tools/list` – Lista las herramientas disponibles.
- `tools/call` – Ejecuta una herramienta específica.
- `resources/list` – Lista los recursos disponibles.
- `resources/read` – Lee el contenido de un recurso específico.
- `ping` – Valida conectividad (requiere haber inicializado previamente).

### Ejemplo de requests JSON-RPC

```bash
curl -sS http://localhost:8080/mcp -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05"}}'
curl -sS http://localhost:8080/mcp -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}'
curl -sS http://localhost:8080/mcp -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":"3","method":"ping","params":{}}'
```

Para facilitar estas pruebas existe el script [`scripts/mcp_smoke.sh`](scripts/mcp_smoke.sh).

## ¿Qué es el Protocolo MCP?

> 📚 Referencias oficiales:
> - [MCP GitHub Repo (langchain4j)](https://github.com/langchain4j/mcp)
> - [LangChain4j - Model Content Protocol Overview](https://github.com/langchain4j/langchain4j/blob/main/docs/model-content-protocol.md)
> - [MCP en LangChain4j Docs](https://docs.langchain4j.dev/integrations/mcp/)
>
> 📖 Recursos complementarios:
> - [LangChain JS - Model Content Protocol](https://js.langchain.com/docs/integrations/model_content_protocol/)
> - [Artículo: “Serving Chunks and Metadata with MCP”](https://medium.com/@langchain/serving-chunks-and-metadata-to-rag-apps-ec7b9a0c1d13)
> - [Video explicativo: What is MCP? (YouTube)](https://www.youtube.com/watch?v=9frCYmBOAlY)

![MCP Diagram](./docs/images/mcp.png)

**MCP (Model Content Protocol)** es un protocolo abierto y estandarizado que permite a los servidores exponer:
- Estructura de metadatos (metadata) que describe tablas, columnas, relaciones, tipos y descripciones.
- Contenido textual dividido en “chunks” que puede ser embebido, indexado y consultado por modelos de lenguaje (LLMs).

Este protocolo facilita la interoperabilidad entre múltiples fuentes de datos (SQL, APIs, archivos...) y consumidores inteligentes como sistemas RAG, agentes autónomos, asistentes virtuales o dashboards inteligentes.

### ¿Qué define un MCP Server?

Un servidor MCP debe cumplir con las siguientes interfaces públicas:
- `GET /mcp/metadata`: entrega metadata completa del dominio conectado.
- `GET /mcp/metadata/short`: devuelve un resumen liviano para inferencia estructural rápida.
- `GET /mcp/chunks`: expone contenidos con texto, metadatos y paginación para alimentar motores RAG o generativos.

Además, los objetos expuestos siguen estructuras comunes como `ChunkDto`, `TableMetadataDto` o `DatabaseMetadataDto`.

> ✅ Uno de los objetivos principales del MCP es desacoplar completamente el consumidor (cliente RAG) del proveedor (fuente de datos), permitiendo evolución y composición sin fricción.

---


## ¿Cómo MELIAN adhiere al estándar MCP?

MELIAN implementa el estándar MCP con total compatibilidad:

- `GET /mcp/metadata` y `GET /mcp/metadata/short`: expone metadata rica o resumida.
- `GET /mcp/chunks`: entrega datos chunkeados con soporte para paginación, filtros, y múltiples fuentes.
- Utiliza DTOs compatibles con MCP: `ChunkDto`, `DatabaseMetadataDto`, `TableMetadataDto`, etc.

Además, MELIAN permite elegir la fuente de datos (`source=sql` o `source=rest`) de forma declarativa, siendo agnóstico del origen.

---

## Arquitectura MELIAN (MCP Multi-backend)

MELIAN implementa una arquitectura multi-backend, donde puede consultar y federar datos de múltiples fuentes (SQL, MongoDB, TMDB, archivos, etc.) y exponerlos a través del protocolo MCP. No implementa capacidades de asistente IA ni integración directa con OpenAI o LangChain4j AiServices.

- **Federación de datos**: Consulta y unifica datos de películas desde diferentes repositorios.
- **Exposición estándar**: Expone los datos y metadatos a través de los métodos MCP (`tools/list`, `tools/call`, `resources/list`, `resources/read`, etc.).
- **Sin asistente IA**: MELIAN no es un asistente conversacional ni integra modelos de lenguaje de manera nativa.

---

## Diagrama de Secuencia: `/mcp/metadata`

![MetadataController Sequence](./docs/images/mcp-seq.png)

---

## Workflow desde un Cliente RAG

![Workflow Cliente RAG](./docs/images/mcp.png)

---

## ¿Para qué sirve MELIAN?

- Exponer cualquier fuente de datos de manera semántica y federada (SQL, archivos, planillas, APIs…).
- Generar embeddings “al vuelo” o batch para queries en lenguaje natural (NLQ) o estructuradas.
- Unificar acceso, chunking y lógica en un solo punto configurable por negocio/área.
- Potenciar plataformas RAG, chatbots y soluciones de IA con contexto relevante, auditado y gobernado.

---

## Instancias y “sabores” de MELIAN

Cada implementación de MELIAN puede ser adaptada al área, negocio o dominio:

| Instancia          | Descripción                                                                            |
| ------------------ | -------------------------------------------------------------------------------------- |
| MELIAN-Finanzas    | Centraliza y expone datos financieros, reportes y consultas específicas del área.      |
| MELIAN-Legal       | Conecta documentos legales, reglamentos, contratos y expone chunks preparados para IA. |
| MELIAN-Operaciones | Orquesta información operativa, logs, reportes de procesos y métricas.                 |

¿Querés sumar tu propio “sabor”? Solo cambia la configuración y conecta tus fuentes.

---


---

## Buenas prácticas de implementación

- Usar servicios `@Service` y controladores `@RestController` separados por responsabilidad.
- Evitar lógica en controladores; delegar a servicios y aplicar validaciones tempranas.
- El chunk debe tener un campo `text` bien formateado y `metadata` utilizable como contexto.
- Validar entradas en endpoints (`filter`, `source`, `table`) para evitar SQL Injection.
- Registrar en logs la trazabilidad de las consultas (`limit`, `afterId`, `filter`) para debug e inferencia.

---

## 🚀 ¿Cómo ejecutar el Servidor MCP MELIAN?

### Opción 1: Servidor MCP Puro (Recomendado para MCP compliance)

MELIAN ahora incluye un servidor MCP puro que cumple completamente con el estándar MCP sin dependencias de OpenAI.

```bash
# Compilar el proyecto
mvn clean package -DskipTests

# Ejecutar servidor MCP con transporte HTTP
MCP_SERVER_HTTP_ENABLED=true java -jar target/melian-*.jar

# Ejecutar servidor MCP con transporte Stdio (por defecto)
java -jar target/melian-*.jar

# Ejecutar con ambos transportes
MCP_SERVER_HTTP_ENABLED=true MCP_SERVER_STDIO_ENABLED=true java -jar target/melian-*.jar
```

#### Variables de Entorno para MCP Server:

```bash
# Configuración de transporte
export MCP_SERVER_HTTP_ENABLED=true    # Habilitar transporte HTTP
export MCP_SERVER_STDIO_ENABLED=true   # Habilitar transporte Stdio
export MCP_SERVER_HOST=0.0.0.0         # Host del servidor HTTP
export MCP_SERVER_PORT=3000             # Puerto del servidor HTTP

# Configuración de bases de datos (opcional)
export DB_URL="jdbc:mysql://localhost:3307/sakila"
export DB_USERNAME="sakila"
export DB_PASSWORD="sakila"
export MONGODB_URI="mongodb://root:example@localhost:27017/melian_movies"

# Configuración de TMDB/IMDB
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"

# Modo puro MCP (sin OpenAI)
export MCP_PURE_MODE=true
export DISABLE_OPENAI=true
```

#### Configuración del idioma de búsqueda

El idioma utilizado para las búsquedas en MongoDB se controla mediante la propiedad `melian.search.locale` en `application.yml`.
Por defecto se usa `en`, pero puede cambiarse, por ejemplo, a español:

```yaml
melian:
  search:
    locale: es
```

#### Endpoints MCP Estándar:

- **JSON-RPC**: `POST /mcp` - Endpoint principal MCP con métodos estándar
- **Health**: `GET /mcp/health` - Estado del servidor
- **Tools**: `GET /mcp/tools` - Lista de herramientas disponibles
- **Resources**: `GET /mcp/resources` - Lista de recursos disponibles

#### Métodos MCP Soportados:

1. **initialize** - Inicializar conexión MCP
2. **tools/list** - Listar herramientas disponibles
3. **tools/call** - Ejecutar herramienta específica
4. **resources/list** - Listar recursos disponibles
5. **resources/read** - Leer contenido de recurso específico

#### Herramientas MCP Disponibles:

- `search_movies` - Buscar películas usando TMDB API
- `get_movie_chunks` - Obtener chunks de datos de películas para RAG
- `get_server_status` - Obtener estado del servidor

### Opción 2: Con Docker Compose (Recomendado para desarrollo)

```bash
# Levantar todo el stack (MySQL, MongoDB, MCP Server)
docker-compose up -d

# Ver logs del servidor MCP
docker-compose logs -f melian-mcp-server

# Probar el servidor
./test-mcp-server.sh
```

### Opción 3: Asistente de IA MELIAN (Legacy)

Para compatibilidad hacia atrás, el asistente de IA original sigue disponible:

### Opción 3: Script Automático (Legacy - AI Assistant)

```bash
# Hacer el script ejecutable (solo la primera vez)
chmod +x run-mcp-server.sh

# Ejecutar con asistente interactivo
./run-mcp-server.sh
```

El script te guiará paso a paso para:
- ✅ Verificar requisitos (Java 17+)
- ✅ Compilar si es necesario
- ✅ Configurar bases de datos opcionales
- ✅ Levantar Docker si es requerido
- ✅ Configurar token TMDB
- ✅ Configurar OpenAI API Key (opcional)
- ✅ Ejecutar el asistente

### Opción 4: Ejecución Manual (Legacy - AI Assistant)

#### 1. Requisitos previos:
- **Java 17+** (`java --version`)
- **Maven 3.8+** (solo para compilación)
- **Docker** (opcional, para MongoDB/MySQL)
- **Token TMDB** (opcional, para búsquedas reales)
- **OpenAI API Key** (opcional, para modo IA completo)

#### 2. Compilar el proyecto:
```bash
mvn clean package -DskipTests
```

#### 3. Ejecución básica (modo herramientas solamente):
```bash
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

#### 4. Ejecución con IA completa:
```bash
# OpenAI API Key (para modo IA completo)
export OPENAI_API_KEY="tu_openai_api_key_aqui"

# Token TMDB (recomendado)
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"

# Ejecutar asistente
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

#### 5. Con bases de datos externas (opcional):
```bash
# Base de datos MySQL (opcional)
export DB_URL="jdbc:mysql://localhost:3307/sakila"
export DB_USERNAME="sakila"
export DB_PASSWORD="sakila"

# MongoDB (opcional)
export MONGODB_URI="mongodb://root:example@localhost:27017"
export MONGODB_DATABASE="melian"

# Habilitar servidor MCP de archivos (opcional)
export ENABLE_FILESYSTEM_MCP="true"

# Ejecutar asistente
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

#### 6. Con Docker (bases de datos completas):
```bash
# Levantar bases de datos
docker-compose up -d

# Configurar variables
export OPENAI_API_KEY="tu_openai_api_key_aqui"
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"

# Ejecutar asistente
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

### ✅ Verificación del asistente

#### Modo Herramientas (sin OpenAI API Key):
```
🔧 MELIAN Tool Mode - No ChatModel available
Available commands:
  search <query> [limit] - Search movies
  chunks <source> [limit] [filter] - Get movie chunks
  status - Get server status
  quit/exit - Stop
```

#### Modo IA Completo (con OpenAI API Key):
```
🎬 MELIAN AI Assistant ready! Ask me about movies or type 'help' for commands.

> Tell me about Matrix movies
> Search for comedy movies from 2020
> Get movie data chunks about action films
```

### 📖 Documentación detallada

Para instrucciones completas, troubleshooting y configuración avanzada:
- **[📘 Guía Completa de Ejecución](./EJECUTAR_SERVIDOR_MCP.md)**

### 🎯 Funcionalidades del Asistente IA

El asistente proporciona:
- 🤖 **Asistente de IA Conversacional**:
  - Chat natural sobre películas
  - Búsqueda inteligente de contenido
  - Análisis de datos de películas
- 🔧 **3 Herramientas Integradas**:
  - `searchMovies`: Búsqueda de películas usando TMDB API
  - `getMovieChunks`: Obtener chunks de datos para aplicaciones RAG
  - `getServerStatus`: Estado y configuración del servidor
- 🌐 **Integración MCP Externa**: Conexión opcional a servidores MCP externos
- 🗄️ **Múltiples fuentes**: SQL (H2/MySQL), MongoDB, TMDB API
- 🚀 **Modo Dual**: Funciona con o sin OpenAI API Key

---

## Roadmap

---


## 🟢 Cumplimiento MCP (Model Content Protocol)

MELIAN es **MCP compliant** y puede ser consumido por clientes MCP estándar como Claude Desktop, VS Code, LangChain4j, etc. Cumple con:

- Exposición de endpoints y métodos estándar MCP (REST y JSON-RPC)
- Estructura de chunks (`ChunkDto`) y metadata (`TableMetadataDto`, `DatabaseMetadataDto`) según el estándar
- Soporte de paginación, filtros y selección de fuente (`source=sql`, `source=mongo`, `source=tmdb`)
- Respuestas y errores alineados a la especificación MCP
- Interoperabilidad con clientes RAG y asistentes IA que soporten MCP

> Para más detalles sobre el estándar MCP, consulta la [documentación oficial](https://github.com/langchain4j/langchain4j/blob/main/docs/model-content-protocol.md).

---

## Mejoras técnicas recomendadas: consultas paralelas y federación moderna

Para optimizar la consulta a múltiples backends y hacer MELIAN aún más elegante y eficiente, se recomienda:

- **Uso de GraphQL federado**: Implementar una capa interna GraphQL (con Apollo Federation, GraphQL Mesh, etc.) para federar y consultar en paralelo múltiples fuentes (SQL, MongoDB, TMDB, archivos, etc.). Los resolvers pueden ejecutarse en paralelo y exponer un esquema unificado.
- **Programación reactiva (WebFlux, Project Reactor, RxJava)**: Migrar los adaptadores a un modelo reactivo (`Flux`/`Mono`), permitiendo consultas no bloqueantes y paralelas a los distintos repositorios. Esto reduce la latencia y mejora la escalabilidad.
- **Uso de CompletableFuture o Parallel Streams**: Para una transición progresiva, se pueden lanzar consultas en paralelo usando `CompletableFuture.supplyAsync()` o streams paralelos en Java.
- **Ventajas**: Menor latencia, mayor escalabilidad, código más moderno y mantenible, y preparado para arquitecturas serverless o microservicios.
- **Impacto en el diagrama**: La capa de adaptadores multi-backend puede etiquetarse como "Reactive Streams" o "GraphQL Federation" para reflejar la consulta federada y paralela.

Estas mejoras permiten que MELIAN aproveche tecnologías modernas para federar y consultar datos de múltiples fuentes de manera eficiente y elegante, manteniendo la fachada MCP estándar para los clientes.
