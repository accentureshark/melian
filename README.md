# MELIAN

> **Módulo de Embedding y Lógica Inteligente para Acceso Natural**

---

![melian-logo](./assets/melian-logo.png)

---

## ¿Qué es MELIAN?

**MELIAN** es tu asistente digital abstracto para la integración y exposición inteligente de datos empresariales.  
No es solo un software: es un *personaje* que entiende, transforma y sirve conocimiento a cualquier dominio de tu
organización.

MELIAN se implementa como un **MCP Server** (Model Content Protocol), capaz de conectarse a bases de datos, archivos,
APIs, documentos, planillas y sistemas legacy,  
y exponer chunks enriquecidos, embeddings y metadata lista para potenciar aplicaciones RAG y clientes de IA
como [EvolvAI](https://github.com/tu-org/evolvai).

---

## ¿Qué es el Protocolo MCP?

![MCP Diagram](./docs/images/mcp.png)

**MCP (Model Content Protocol)** es un protocolo estandarizado para exponer metadatos y contenido chunkeado, preparado
para consumirse por clientes RAG, agentes de IA y motores de inferencia.  
Un servidor MCP debe:

- Exponer una estructura de metadatos accesible vía `/mcp/metadata`
- Exponer contenidos divididos en chunks vía `/mcp/chunks`
- Utilizar estructuras de datos como `ChunkDto` y `DatabaseMetadataDto` según el estándar
- Mantener compatibilidad con diversas fuentes y formatos de datos (SQL, REST, archivos...)

MCP no impone un backend único, sino una interfaz común.

---

## ¿Cómo MELIAN adhiere al estándar MCP?

MELIAN implementa el estándar MCP con total compatibilidad:

- `GET /mcp/metadata` y `GET /mcp/metadata/short`: expone metadata rica o resumida.
- `GET /mcp/chunks`: entrega datos chunkeados con soporte para paginación, filtros, y múltiples fuentes.
- Utiliza DTOs compatibles con MCP: `ChunkDto`, `DatabaseMetadataDto`, `TableMetadataDto`, etc.

Además, MELIAN permite elegir la fuente de datos (`source=sql` o `source=rest`) de forma declarativa, siendo agnóstico
del origen.

---

## Arquitectura Flexible

![MCP Server Components](./docs/images/MCPServerComponents.png)

**Descripción:**

- MELIAN enruta solicitudes según el origen de datos (`source`).
- Usa un `ChunkService` y `MetadataService` intercambiables.
- Se puede extender a nuevos backends (Mongo, GraphQL, CSV...) sin modificar los controladores.

---

## Diagrama de Secuencia: `/mcp/metadata`

![MetadataController Sequence](./docs/images/MetadataControllerSequence.png)

---

## Diagrama de Secuencia: `/mcp/chunks`

![ChunkController Sequence](./docs/images/ChunkControllerSequence.png)

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
|--------------------|----------------------------------------------------------------------------------------|
| MELIAN-Finanzas    | Centraliza y expone datos financieros, reportes y consultas específicas del área.      |
| MELIAN-Legal       | Conecta documentos legales, reglamentos, contratos y expone chunks preparados para IA. |
| MELIAN-Operaciones | Orquesta información operativa, logs, reportes de procesos y métricas.                 |

¿Querés sumar tu propio “sabor”? Solo cambia la configuración y conecta tus fuentes.

---

## Roadmap

- ✅ Soporte para SQL con chunking y metadata enriquecida
- ✅ Integración con APIs REST externas como TMDB
- 🔄 Embedding vía LangChain4j, Ollama, OpenAI, etc.
- 🔜 Plugin system para reglas de negocio
- 🔜 Documentación y ejemplos para cada “sabor” MELIAN
