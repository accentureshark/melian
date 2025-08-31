#!/bin/bash

# Simple script to test MELIAN streaming functionality

echo "🚀 MELIAN Streaming Test"
echo "========================="

# Build
echo "📦 Building streaming application..."
mvn package -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

echo "✅ Build successful"

# Start the streaming application
echo "🎬 Starting MELIAN Streaming Server..."
echo "📝 Server will start on http://localhost:8080"
echo "📝 Streaming endpoints:"
echo "   - POST /api/chat/message (regular chat)"
echo "   - POST /api/chat/stream (streaming chat with SSE)"
echo "   - GET /api/chat/status (server status)"
echo ""

# Run with simplified configuration
java -Dspring.profiles.active=streaming \
     -cp target/melian-0.1.0-SNAPSHOT.jar \
     org.shark.melian.StreamingMelianApplication