.PHONY: run-mcp up build down build-client run-client-backend run-client-ui run-client clean-client

# Main MCP Server targets
run-mcp:
	java -jar target/melian-*.jar

up:
	docker-compose up -d

build:
	docker-compose  up --build

down:
	docker-compose down

local:
	docker-compose up -d mongo mysql-sakila

# Client targets
build-client: build-client-backend build-client-ui

build-client-backend:
	cd mcp-client/backend && mvn clean package

build-client-ui:
	cd mcp-client/ui && npm install && npm run build

run-client-backend:
	cd mcp-client/backend && mvn spring-boot:run

run-client-ui:
	cd mcp-client/ui && npm run dev

# Run both client components (backend + UI)
run-client: build-client-backend
	@echo "Starting MCP Client Backend and UI..."
	@echo "Backend will run on http://localhost:8083"
	@echo "UI will run on http://localhost:5173"
	@echo "Press Ctrl+C to stop both services"
	(cd mcp-client/backend && mvn spring-boot:run) & \
	(cd mcp-client/ui && npm run dev) & \
	wait

clean-client:
	cd mcp-client/backend && mvn clean
	cd mcp-client/ui && rm -rf node_modules dist