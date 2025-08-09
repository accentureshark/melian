#!/bin/bash

echo "🚀 MELIAN MCP Server - Execution Script"
echo "=========================================="
echo ""

# Verify Java
echo "📋 Verifying requirements..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17+ first."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Found Java $JAVA_VERSION. Java 17+ is required."
    exit 1
fi

echo "✅ Java $JAVA_VERSION OK"

# Verificar JAR
JAR_FILE="target/melian-0.1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR not found: $JAR_FILE"
    echo "🔧 Building project..."
    mvn clean package -DskipTests -q
    if [ $? -ne 0 ]; then
        echo "❌ Error building the project"
        exit 1
    fi
    echo "✅ Project built successfully"
else
    echo "✅ JAR found: $JAR_FILE"
fi

echo ""

# Check configuration
echo "⚙️ Current configuration:"
echo "- Database: ${DB_URL:-jdbc:h2:mem:melian (H2 in-memory)}"
echo "- MongoDB: ${MONGODB_URI:-mongodb://localhost:27017 (default)}"
echo "- TMDB Token: ${TMDB_ACCESS_TOKEN:+Configured ✅}"
if [ -z "$TMDB_ACCESS_TOKEN" ]; then
    echo "- TMDB Token: ❌ Not configured (limited searches)"
fi

echo ""

# Ask if they want to configure TMDB
if [ -z "$TMDB_ACCESS_TOKEN" ]; then
    echo "🎬 Do you have a TMDB token for movie searches? (y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "Enter your TMDB Access Token:"
        read -r tmdb_token
        export TMDB_ACCESS_TOKEN="$tmdb_token"
        echo "✅ TMDB Token configured"
    fi
fi

echo ""

# Show options
echo "🎛️ Execution options:"
echo "1. Basic (H2 in-memory)"
echo "2. With MongoDB (requires Docker)"
echo "3. With MySQL (requires Docker)"
echo "4. Complete (MySQL + MongoDB + Docker)"
echo "5. HTTP SSE (expose remote server)"
echo ""
echo "Select an option (1-5) or press Enter for basic:"
read -r option

case "$option" in
    2)
        echo "🐳 Verifying MongoDB..."
        if ! docker ps | grep -q mongo; then
            echo "📦 Starting MongoDB..."
            docker-compose -f mongodb-docker-compose.yml up -d
            sleep 5
        fi
        export MONGODB_URI="mongodb://root:example@localhost:27017"
        echo "✅ MongoDB configured"
        ;;
    3)
        echo "🐳 Verifying MySQL..."
        if ! docker ps | grep -q mysql; then
            echo "📦 Starting MySQL..."
            docker-compose up -d mysql-sakila
            sleep 10
        fi
        export DB_URL="jdbc:mysql://localhost:3307/sakila"
        export DB_USERNAME="sakila"
        export DB_PASSWORD="sakila"
        export DB_DRIVER="com.mysql.cj.jdbc.Driver"
        echo "✅ MySQL configured"
        ;;
    4)
        echo "🐳 Verificando bases de datos..."
        if ! docker ps | grep -q mysql || ! docker ps | grep -q mongo; then
            echo "📦 Levantando todas las bases de datos..."
            docker-compose up -d
            sleep 15
        fi
        export DB_URL="jdbc:mysql://localhost:3307/sakila"
        export DB_USERNAME="sakila"
        export DB_PASSWORD="sakila"
        export DB_DRIVER="com.mysql.cj.jdbc.Driver"
        export MONGODB_URI="mongodb://root:example@localhost:27017"
        echo "✅ MySQL y MongoDB configurados"
        ;;
    5)
        echo "🐳 Verificando bases de datos..."
        if ! docker ps | grep -q mysql || ! docker ps | grep -q mongo; then
            echo "📦 Levantando todas las bases de datos..."
            docker-compose up -d
            sleep 15
        fi
        export DB_URL="jdbc:mysql://localhost:3307/sakila"
        export DB_USERNAME="sakila"
        export DB_PASSWORD="sakila"
        export DB_DRIVER="com.mysql.cj.jdbc.Driver"
        export MONGODB_URI="mongodb://root:example@localhost:27017"
        echo "✅ MySQL y MongoDB configurados"
        echo "🌐 Starting in HTTP SSE mode"
        export MCP_SERVER_HTTP_ENABLED=true
        ;;
    *)
        echo "✅ Using basic configuration (H2 in-memory)"
        ;;
esac

echo ""
echo "🚀 Starting MELIAN MCP Server..."
echo "📝 To stop the server: Ctrl+C"
echo "📖 Full documentation: ./RUN_MCP_SERVER.md"
echo ""
echo "=========================================="

# Run the server
java -jar "$JAR_FILE"