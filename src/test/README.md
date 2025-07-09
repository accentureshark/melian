# Melian Test Suite

This test suite provides comprehensive unit testing for the Melian MCP Server application.

## Test Coverage

### Service Layer Tests
- **TMDBServiceTest** - Tests for TMDB API integration service
- **TMDBMovieToolServiceTest** - Tests for AI tool service for movie operations  
- **SqlMetadataServiceTest** - Tests for SQL database metadata extraction
- **MongoMetadataServiceTest** - Tests for MongoDB metadata extraction
- **RestApiMetadataServiceTest** - Tests for REST API virtual table metadata
- **SqlChunkServiceSimpleTest** - Basic tests for SQL chunk operations

### Controller Layer Tests
- **MetadataControllerTest** - Tests for metadata REST endpoints
- **ChunkControllerTest** - Tests for chunk data REST endpoints  
- **MovieControllerTest** - Tests for movie-specific REST endpoints

### Application Tests
- **MelianApplicationTest** - Integration test for Spring application context

## Test Configuration

The test suite includes:
- Test-specific configuration with H2 in-memory database
- Mock configurations for external dependencies
- Test data setup for SQL operations
- Proper isolation between test cases using Mockito

## Running Tests

To run all tests:
```bash
mvn test
```

To run specific test classes:
```bash
mvn test -Dtest="TMDBServiceTest"
```

To run tests with coverage:
```bash
mvn test jacoco:report
```

## Test Principles

- **Unit Testing**: Each test focuses on a single unit of functionality
- **Mocking**: External dependencies are mocked to ensure test isolation
- **Comprehensive Coverage**: Tests cover both happy path and error scenarios
- **Maintainable**: Tests are simple, readable, and follow consistent patterns