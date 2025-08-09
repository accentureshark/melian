#!/bin/bash
# Simple smoke test for MCP JSON-RPC endpoint
set -euo pipefail

curl -sS http://localhost:8080/mcp -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05"}}'
echo
curl -sS http://localhost:8080/mcp -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{"cursor":null,"limit":100}}'
echo
curl -sS http://localhost:8080/mcp -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":"3","method":"ping","params":{}}'
echo
