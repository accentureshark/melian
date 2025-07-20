.PHONY: run-mcp up build down

run-mcp:
	java -jar target/melian-*.jar

up:
	docker-compose up -d

build:
	docker-compose  up --build

down:
	docker-compose down