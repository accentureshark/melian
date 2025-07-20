
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

## Arquitectura Refactorizada (Sin Spring Boot)

MELIAN ahora utiliza el **SDK oficial de MCP de Java** (`io.modelcontextprotocol.sdk:mcp:0.10.0`) en lugar de Spring Boot, proporcionando:

- ✅ **MCP Compliance Nativo**: Implementación directa del protocolo MCP sin abstracciones adicionales
- ✅ **Menor Overhead**: Sin dependencias de Spring Boot ni framework web
- ✅ **Startup Rápido**: Inicio más rápido al eliminar el contenedor Spring
- ✅ **Menor Tamaño**: JAR más pequeño y eficiente en memoria
- ✅ **Pure Java**: Configuración basada en código Java puro sin anotaciones mágicas

### Componentes Principales:

- **MelianMcpServer**: Servidor principal usando MCP SDK oficial
- **Pure Services**: Servicios sin dependencias de Spring (TMDBServicePure, SqlMovieChunkServicePure, MongoMovieChunkServicePure)
- **Configuration**: Gestión de configuración basada en properties y variables de entorno
- **STDIO Transport**: Comunicación MCP nativa vía STDIN/STDOUT

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

## ¿Cómo levantar el servidor MELIAN?

1. Requisitos:
    - Java 17+
    - Maven 3.8+ (solo para compilación)
    - Docker (opcional, para levantar base de datos como MongoDB)
    - MySQL local o remoto (opcional)

2. Compilar y empaquetar el servidor:

```bash
mvn clean package -DskipTests
```

3. Ejecutar el servidor MCP usando el JAR standalone:

```bash
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

4. Usar variables de entorno para configuración:

```bash
# Configurar TMDB API
export TMDB_ACCESS_TOKEN="tu_token_tmdb"

# Configurar base de datos (opcional)
export DB_URL="jdbc:mysql://localhost:3306/melian"
export DB_USERNAME="usuario"
export DB_PASSWORD="contraseña"

# Configurar MongoDB (opcional)
export MONGODB_URI="mongodb://localhost:27017"
export MONGODB_DATABASE="melian"

# Ejecutar el servidor
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

5. Endpoints MCP disponibles:

El servidor implementa el protocolo MCP estándar vía STDIO. Para probarlo desde línea de comandos, puede conectarse a través de STDIN/STDOUT.

El servidor proporciona acceso a:
- Búsqueda de películas desde TMDB API
- Almacenamiento en SQL (H2 en memoria por defecto)
- Almacenamiento en MongoDB
- Recuperación de chunks de datos para sistemas RAG

---

## Roadmap

