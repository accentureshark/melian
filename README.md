# MELIAN

> **Módulo de Embedding y Lógica Inteligente para Acceso Natural**

---

![melian-logo](./assets/melian-logo.png) <!-- Puedes agregar un logo cuando lo tengas -->

---

## ¿Qué es MELIAN?

**MELIAN** es tu asistente digital abstracto para la integración y exposición inteligente de datos empresariales.  
No es solo un software: es un *personaje* que entiende, transforma y sirve conocimiento a cualquier dominio de tu organización.

MELIAN se implementa como un **MCP Server** (Model Content Protocol), capaz de conectarse a bases de datos, archivos, APIs, documentos, planillas y sistemas legacy,  
y exponer chunks enriquecidos, embeddings y metadata lista para potenciar aplicaciones RAG y clientes de IA como [EvolvAI](https://github.com/tu-org/evolvai).

---

## Filosofía MELIAN

- **Abstracta y adaptable:**  
  MELIAN no tiene forma única; se “viste” según la necesidad del área: finanzas, legal, operaciones, compliance…
- **Orquestadora de lógica:**  
  Conecta fuentes, aplica reglas, curaciones y chunking inteligente, generando embeddings listos para IA.
- **Naturalmente conectable:**  
  Expone sus datos y lógica a través del estándar MCP, para que cualquier cliente (humano o IA) acceda de manera natural y segura.

---

## ¿Para qué sirve MELIAN?

- Exponer cualquier fuente de datos de manera semántica y federada (SQL, archivos, planillas, APIs…).
- Generar embeddings “al vuelo” o batch para queries en lenguaje natural (NLQ) o estructuradas.
- Unificar acceso, chunking y lógica en un solo punto configurable por negocio/área.
- Potenciar plataformas RAG, chatbots y soluciones de IA con contexto relevante, auditado y gobernado.

---

## Instancias y “sabores” de MELIAN

Cada implementación de MELIAN puede ser adaptada al área, negocio o dominio:

| Instancia         | Descripción                                     |
|-------------------|------------------------------------------------|
| MELIAN-Finanzas   | Centraliza y expone datos financieros, reportes y consultas específicas del área. |
| MELIAN-Legal      | Conecta documentos legales, reglamentos, contratos y expone chunks preparados para IA. |
| MELIAN-Operaciones| Orquesta información operativa, logs, reportes de procesos y métricas.             |

¿Querés sumar tu propio “sabor”? Solo cambia la configuración y conecta tus fuentes.

---

## Arquitectura (Resumen)

```plantuml
@startuml
actor "Cliente (RAG, IA, Usuario)" as client

rectangle "MELIAN (MCP Server)" {
  [API MCP (NLQ/SQL)]
  [Catálogo de Esquema / Metadata]
  [Chunker y Enriquecedor]
  [Embedding Service]
  [MCP Serializer]
}

client --> [API MCP (NLQ/SQL)] : Consulta (NLQ/SQL)
[API MCP (NLQ/SQL)] --> [Catálogo de Esquema / Metadata]
[API MCP (NLQ/SQL)] --> [Chunker y Enriquecedor]
[Chunker y Enriquecedor] --> [Embedding Service]
[Embedding Service] --> [MCP Serializer]
[MCP Serializer] --> client : Respuesta MCP (chunks + embeddings + metadata)

@enduml


## Roadmap
 Soporte para SQL con chunking y metadata enriquecida

 Integración con filesystems y APIs REST

 Embedding vía LangChain4j, Ollama, OpenAI, etc.

 Plugin system para reglas de negocio

 Documentación y ejemplos para cada “sabor” MELIAN
