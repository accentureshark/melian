# MCP-Compliant Movie Services

This document explains the new MCP-compliant movie services implemented in MELIAN.

## Overview

The movie services have been redesigned to follow MCP (Model Content Protocol) standards while maintaining simplicity and reducing code complexity. The implementation now provides clean separation between external API calls and data storage operations.

## Architecture

### Services

1. **MovieChunkService (Interface)**: Defines MCP-compliant operations for movie data
2. **SqlMovieChunkService**: SQL implementation with H2/MySQL support
3. **MongoMovieChunkService**: MongoDB implementation
4. **TMDBService**: Simplified external API client (only API calls)
5. **MovieToolService**: AI tools for Spring AI integration

### Controllers

1. **MovieController**: REST API for movie operations (`/mcp/movies/`)
2. **ChunkController**: Enhanced to support movie chunks (`/mcp/chunks?table=movies`)

## Features

### MCP Compliance

All movie data is stored and retrieved as MCP-compliant chunks with:
- **id**: Unique identifier
- **text**: Human-readable movie description
- **metadata**: Structured movie data (title, overview, rating, etc.)

### Storage Options

- **SQL**: Automatic table creation, upsert operations
- **MongoDB**: Collection-based storage with flexible schema

### AI Tools Available

```java
@Tool("search_movies_by_tmdb_api") // Basic TMDB search
@Tool("search_and_store_movies_sql") // Search + store in SQL
@Tool("search_and_store_movies_mongo") // Search + store in MongoDB
@Tool("get_stored_movies_sql") // Retrieve from SQL as chunks
@Tool("get_stored_movies_mongo") // Retrieve from MongoDB as chunks
```

## REST API Endpoints

### Search Movies
```
GET /mcp/movies/search?title=avatar&limit=5&store=true&storage=sql
```

### Get Movie Chunks
```
GET /mcp/movies/chunks?storage=sql&source=tmdb&limit=10&filter=title like 'avatar'
```

### Store Movies Manually
```
POST /mcp/movies/store?source=manual&storage=mongo
Content-Type: application/json

[{"title": "Movie Title", "overview": "Description", ...}]
```

### Access via Chunks API
```
GET /mcp/chunks?table=movies&source=sql&limit=10
```

## Usage Examples

### 1. Search and Store Movies

```bash
# Search for movies and store in SQL database
curl "http://localhost:8080/mcp/movies/search?title=inception&store=true&storage=sql"

# Search for movies and store in MongoDB
curl "http://localhost:8080/mcp/movies/search?title=matrix&store=true&storage=mongo"
```

### 2. Retrieve Stored Movies as MCP Chunks

```bash
# Get movies from SQL as chunks
curl "http://localhost:8080/mcp/movies/chunks?storage=sql&filter=rating>7.0"

# Get movies from MongoDB as chunks
curl "http://localhost:8080/mcp/movies/chunks?storage=mongo&filter=title like 'star'"
```

### 3. Use with AI Tools

The MovieToolService provides AI tools that can be called by Spring AI:
- Search movies from TMDB API
- Search and automatically store in SQL/MongoDB
- Retrieve stored movies as MCP chunks for RAG operations

## Configuration

The services use existing configuration:

```yaml
# TMDB API
tmdb:
  api-url: https://api.themoviedb.org/3
  access-token: ${TMDB_ACCESS_TOKEN}

# SQL Database (H2/MySQL)
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # or MySQL URL
    
# MongoDB
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/melian
```

## Benefits

1. **MCP Compliance**: All data follows standard chunk format for AI/RAG systems
2. **Reduced Complexity**: Clear separation of API calls vs. storage
3. **Flexible Storage**: Support for both SQL and NoSQL backends
4. **AI Ready**: Integrated with Spring AI tools for LLM interactions
5. **REST API**: Standard endpoints following existing patterns
6. **Automatic Setup**: SQL tables created automatically, MongoDB collections as needed

## Migration from Previous Version

The previous MovieToolService and TMDBService continue to work unchanged. The new services add functionality without breaking existing code:

- Old: `searchMovies()` - Still available, just searches TMDB
- New: `searchAndStoreMoviesSQL()` - Search + store in SQL
- New: `searchAndStoreMoviesMongo()` - Search + store in MongoDB
- New: `getStoredMoviesSQL()` - Retrieve as MCP chunks from SQL
- New: `getStoredMoviesMongo()` - Retrieve as MCP chunks from MongoDB