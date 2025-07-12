# Integration Tests for Melian MCP Server

This directory contains comprehensive integration tests for the Melian MCP Server that test the complete system with real databases and external services.

## Overview

The integration tests verify that the MCP server can properly:
- Access and store data in MySQL databases
- Access and store data in MongoDB databases  
- Integrate with external REST APIs (TMDB)
- Provide MCP tools that work end-to-end
- Handle concurrent operations across multiple services

## Test Structure

### Base Classes
- `BaseIntegrationTest` - Base class with TestContainers setup for MySQL and MongoDB
- `IntegrationTestConfig` - Test configuration with mocked external dependencies

### Integration Test Classes
- `McpServerIntegrationTest` - Tests MCP server functionality with real databases
- `MySqlIntegrationTest` - Tests MySQL database operations in isolation
- `MongoDbIntegrationTest` - Tests MongoDB database operations in isolation
- `RestApiIntegrationTest` - Tests REST API integration with mocked external services
- `FullStackIntegrationTest` - Comprehensive tests of all services working together

## Prerequisites

### Docker Required
The integration tests use TestContainers to start real MySQL and MongoDB instances. You must have Docker installed and running.

### System Requirements
- Java 17+
- Maven 3.6+
- Docker 20+
- At least 4GB RAM available for containers

## Running Integration Tests

### Run All Integration Tests
```bash
mvn verify
```

### Run Only Unit Tests (excludes integration tests)
```bash
mvn test
```

### Run Only Integration Tests
```bash
mvn test -Dgroups=integration
```

### Run Specific Integration Test Class
```bash
mvn test -Dtest=McpServerIntegrationTest
```

### Run Integration Tests with Debug Logging
```bash
mvn test -Dgroups=integration -Dspring.profiles.active=integration -Dlogging.level.org.shark.melian=DEBUG
```

## Test Scenarios Covered

### MCP Server Integration
- ✅ MCP server starts successfully with real database connections
- ✅ MCP tools work with real MySQL database
- ✅ MCP tools work with real MongoDB database
- ✅ MCP tools integrate with mocked REST API
- ✅ Cross-service data consistency

### MySQL Database Integration
- ✅ Connection to real MySQL database
- ✅ Movie data storage and retrieval
- ✅ Chunk-based data access
- ✅ Filtering and pagination
- ✅ Upsert functionality
- ✅ Data integrity validation

### MongoDB Database Integration
- ✅ Connection to real MongoDB database
- ✅ Movie data storage and retrieval
- ✅ Document-based chunk access
- ✅ MongoDB-specific queries and filters
- ✅ ObjectId-based pagination
- ✅ Data consistency validation

### REST API Integration
- ✅ External API calls (mocked for consistency)
- ✅ Response parsing and mapping
- ✅ Error handling and edge cases
- ✅ Rate limiting and performance

### Full Stack Integration
- ✅ Complete workflow testing
- ✅ Concurrent operations
- ✅ Data persistence across services
- ✅ System performance validation
- ✅ End-to-end MCP tool functionality

## Test Data

The integration tests use:
- **TestContainers** for real database instances
- **Mock TMDB API** for consistent external API responses
- **Sample movie data** from The Matrix franchise
- **Temporary test databases** that are cleaned up after each test

## Configuration

### Test Profiles
- `integration` - Activates integration test configuration
- Uses `application-integration.yml` for test-specific settings

### Database Configuration
- MySQL: Automatically configured via TestContainers
- MongoDB: Automatically configured via TestContainers
- Both databases use temporary containers that are destroyed after tests

### External API Configuration
- TMDB API is mocked using Spring Boot test configuration
- Mock returns consistent Matrix movie data for all queries

## Performance Expectations

Integration tests are designed to:
- Complete within 2-3 minutes on average hardware
- Use minimal system resources
- Clean up all containers and data after execution
- Run reliably in CI/CD environments

## Troubleshooting

### Common Issues

**Docker not available:**
```
org.testcontainers.DockerClientException: Could not find a valid Docker environment
```
Solution: Ensure Docker is installed and running

**Port conflicts:**
```
Port 3306 is already in use
```
Solution: Stop other MySQL/MongoDB instances or let TestContainers pick random ports

**Out of memory:**
```
Container failed to start
```
Solution: Increase Docker memory limit or close other applications

### Debug Mode
Enable debug logging to see detailed test execution:
```bash
mvn test -Dgroups=integration -Dlogging.level.org.testcontainers=DEBUG
```

## CI/CD Integration

The integration tests are designed to run in CI/CD pipelines:
- Use the `verify` phase for complete testing
- TestContainers automatically handles container lifecycle
- Tests are isolated and don't interfere with each other
- Proper cleanup ensures no resource leaks

## Contributing

When adding new integration tests:
1. Extend `BaseIntegrationTest` for database access
2. Use the `@IntegrationTest` annotation
3. Use the `integration` profile
4. Follow the existing test patterns
5. Ensure proper cleanup of test data
6. Add documentation for new test scenarios