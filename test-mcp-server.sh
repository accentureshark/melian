#!/bin/bash

# MELIAN MCP Server Test Script
# Tests the pure MCP server implementation via HTTP

echo "🎬 Testing MELIAN MCP Server"
echo "=============================="

# Check if server is running
echo "Checking health endpoint..."
curl -s http://localhost:3000/mcp/health | jq '.'

echo -e "\n\nTesting MCP Initialize..."
curl -s -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {
        "roots": {"listChanged": true}
      },
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    },
    "id": 1
  }' | jq '.'

echo -e "\n\nTesting Tools List..."
curl -s -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": 2
  }' | jq '.'

echo -e "\n\nTesting Search Movies Tool..."
curl -s -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "search_movies",
      "arguments": {
        "query": "matrix",
        "limit": 3
      }
    },
    "id": 3
  }' | jq '.'

echo -e "\n\nTesting Get Movie Chunks Tool..."
curl -s -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_movie_chunks",
      "arguments": {
        "source": "sql",
        "limit": 5
      }
    },
    "id": 4
  }' | jq '.'

echo -e "\n\nTesting Resources List..."
curl -s -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "resources/list",
    "id": 5
  }' | jq '.'

echo -e "\n\nTesting Server Status..."
curl -s -X POST http://localhost:3000/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_server_status",
      "arguments": {}
    },
    "id": 6
  }' | jq '.'

echo -e "\n\n✅ MCP Server test completed!"