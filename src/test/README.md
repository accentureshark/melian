# Melian Test Suite

This test suite provides comprehensive unit and integration testing for the Melian MCP Server application.

## Test Coverage

### Unit Tests

#### Service Layer Tests
- **TMDBServiceTest** - Tests for TMDB API integration service
- **MovieToolServiceTest** - Tests for AI tool service for movie operations  
- **SqlMetadataServiceTest** - Tests for SQL database metadata extraction
- **MongoMetadataServiceTest** - Tests for MongoDB metadata extraction
- **RestApiMetadataServiceTest** - Tests for REST API virtual table metadata
- **SqlChunkServiceSimpleTest** - Basic tests for SQL chunk operations

#### Controller Layer Tests
- **MetadataControllerTest** - Tests for metadata REST endpoints
- **ChunkControllerTest** - Tests for chunk data REST endpoints  
- **MovieControllerTest** - Tests for movie-specific REST endpoints

#### Application Tests
- **MelianApplicationTest** - Integration test for Spring application context

### Integration Tests

#### Database Integration Tests
- **MySqlIntegrationTest** - Tests SQL database operations with real MySQL
- **MongoDbIntegrationTest** - Tests MongoDB operations with real MongoDB
- **McpServerIntegrationTest** - Tests MCP server with real databases

#### API Integration Tests
- **RestApiIntegrationTest** - Tests REST API integration with mock services
- **FullStackIntegrationTest** - End-to-end tests of complete system

## Test Configuration

### Unit Tests
- Test-specific configuration with H2 in-memory database
- Mock configurations for external dependencies
- Test data setup for SQL operations
- Proper isolation between test cases using Mockito

### Integration Tests
- TestContainers for real MySQL and MongoDB databases
- Mock configurations for external APIs
- Real MCP server integration testing
- Comprehensive end-to-end scenarios

## Running Tests

### Unit Tests Only
```bash
mvn test
```

### Integration Tests Only
```bash
mvn verify -Dskip.unit.tests=true
```

### All Tests (Unit + Integration)
```bash
mvn verify
```

### Specific Test Classes
```bash
# Unit test
mvn test -Dtest="TMDBServiceTest"

# Integration test  
mvn test -Dgroups=integration -Dtest="MySqlIntegrationTest"
```

### Tests with Coverage
```bash
mvn verify jacoco:report
```

## Integration Test Requirements

Integration tests require:
- **Docker** installed and running
- **4GB+ RAM** available for containers
- **Network access** for pulling Docker images

See [integration/README.md](java/org/shark/melian/integration/README.md) for detailed integration test documentation.

## Test Principles

- **Unit Testing**: Each test focuses on a single unit of functionality
- **Integration Testing**: Tests complete workflows with real infrastructure
- **Mocking**: External dependencies are mocked to ensure test isolation
- **Comprehensive Coverage**: Tests cover both happy path and error scenarios
- **Maintainable**: Tests are simple, readable, and follow consistent patterns
- **Performance**: Integration tests complete within reasonable time limits