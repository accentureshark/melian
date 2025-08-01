
# MELIAN

> **Módulo de Embedding y Lógica Inteligente para Acceso Natural**

---
<p align="center">
  <img src="./docs/images/melian-logo.png" alt="melian-logo" width="160"/>
</p>
---

## ¿Qué es MELIAN?

**MELIAN** es tu asistente digital abstracto para la integración y exposición inteligente de datos empresariales.  
No es solo un software: es un *personaje* que entiende, transforma y sirve conocimiento a cualquier dominio de tu organización.

MELIAN se implementa como un **MCP Server** (Model Content Protocol), capaz de conectarse a bases de datos, archivos, APIs, documentos, planillas y sistemas legacy,  
y exponer chunks enriquecidos, embeddings y metadata lista para potenciar aplicaciones RAG y clientes de IA como [EvolvAI](https://github.com/tu-org/evolvai).



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

## Arquitectura LangChain4j (Nueva Implementación)

MELIAN ahora utiliza **LangChain4j MCP API** (`dev.langchain4j:langchain4j-mcp:1.2.0-beta8`) en lugar del MCP SDK, convirtiéndose en un asistente de IA que puede integrar múltiples servidores MCP externos. Esto proporciona:

- ✅ **Integración de IA Nativa**: Asistente conversacional usando LangChain4j AiServices
- ✅ **Herramientas de Película Integradas**: Búsqueda TMDB, chunks de datos, estado del servidor
- ✅ **Conexión a Servidores MCP Externos**: Capacidad de conectar múltiples servidores MCP
- ✅ **Modo Dual**: Funciona sin OpenAI (modo herramientas) o con OpenAI (modo IA completo)
- ✅ **Configuración Flexible**: Base de datos opcional, integración externa opcional
- ✅ **Startup Rápido**: H2 en memoria por defecto, sin dependencias externas requeridas

### Componentes Principales:

- **MelianAiAssistant**: Asistente principal usando LangChain4j AiServices
- **MovieTools**: Herramientas de película con anotaciones @Tool de LangChain4j
- **Pure Services**: Servicios sin dependencias externas (TMDBServicePure, SqlMovieChunkServicePure, MongoMovieChunkServicePure)
- **Configuration**: Gestión de configuración basada en properties y variables de entorno
- **MCP Client Integration**: Capacidad de conectar a servidores MCP externos via stdio/HTTP

---

## Diagrama de Secuencia: `/mcp/chunks`

![ChunkController Sequence](./docs/images/ChunkControllerSequence.png)

---

## Diagrama de Secuencia: `/mcp/metadata`

![MetadataController Sequence](./docs/images/MetadataControllerSequence.png)

---

## Workflow desde un Cliente RAG

![Workflow Cliente RAG](./docs/images/workflow_rag.png)

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

## 🚀 ¿Cómo ejecutar el Asistente de IA MELIAN?

### Opción 1: Script Automático (Recomendado)

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

### Opción 2: Ejecución Manual

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

