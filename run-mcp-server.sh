#!/bin/bash

echo "🚀 MELIAN MCP Server - Script de Ejecución"
echo "=========================================="
echo ""

# Verificar Java
echo "📋 Verificando requisitos..."
if ! command -v java &> /dev/null; then
    echo "❌ Java no encontrado. Instalar Java 17+ primero."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java $JAVA_VERSION encontrado. Se requiere Java 17+."
    exit 1
fi

echo "✅ Java $JAVA_VERSION OK"

# Verificar JAR
JAR_FILE="target/melian-0.1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR no encontrado: $JAR_FILE"
    echo "🔧 Compilando proyecto..."
    mvn clean package -DskipTests -q
    if [ $? -ne 0 ]; then
        echo "❌ Error al compilar el proyecto"
        exit 1
    fi
    echo "✅ Proyecto compilado exitosamente"
else
    echo "✅ JAR encontrado: $JAR_FILE"
fi

echo ""

# Verificar configuración
echo "⚙️ Configuración actual:"
echo "- Base de datos: ${DB_URL:-jdbc:h2:mem:melian (H2 en memoria)}"
echo "- MongoDB: ${MONGODB_URI:-mongodb://localhost:27017 (default)}"
echo "- TMDB Token: ${TMDB_ACCESS_TOKEN:+Configurado ✅}"
if [ -z "$TMDB_ACCESS_TOKEN" ]; then
    echo "- TMDB Token: ❌ No configurado (búsquedas limitadas)"
fi

echo ""

# Preguntar si quiere configurar TMDB
if [ -z "$TMDB_ACCESS_TOKEN" ]; then
    echo "🎬 ¿Tienes un token TMDB para búsquedas de películas? (y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        echo "Ingresa tu TMDB Access Token:"
        read -r tmdb_token
        export TMDB_ACCESS_TOKEN="$tmdb_token"
        echo "✅ Token TMDB configurado"
    fi
fi

echo ""

# Mostrar opciones
echo "🎛️ Opciones de ejecución:"
echo "1. Básica (H2 en memoria)"
echo "2. Con MongoDB (requiere Docker)"
echo "3. Con MySQL (requiere Docker)"
echo "4. Completa (MySQL + MongoDB + Docker)"
echo "5. HTTP SSE (exponer servidor remoto)"
echo ""
echo "Selecciona una opción (1-5) o presiona Enter para básica:"
read -r option

case "$option" in
    2)
        echo "🐳 Verificando MongoDB..."
        if ! docker ps | grep -q mongo; then
            echo "📦 Levantando MongoDB..."
            docker-compose -f mongodb-docker-compose.yml up -d
            sleep 5
        fi
        export MONGODB_URI="mongodb://root:example@localhost:27017"
        echo "✅ MongoDB configurado"
        ;;
    3)
        echo "🐳 Verificando MySQL..."
        if ! docker ps | grep -q mysql; then
            echo "📦 Levantando MySQL..."
            docker-compose up -d mysql-sakila
            sleep 10
        fi
        export DB_URL="jdbc:mysql://localhost:3307/sakila"
        export DB_USERNAME="sakila"
        export DB_PASSWORD="sakila"
        export DB_DRIVER="com.mysql.cj.jdbc.Driver"
        echo "✅ MySQL configurado"
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
        echo "🌐 Iniciando en modo HTTP SSE"
        export MCP_SERVER_HTTP_ENABLED=true
        ;;
    *)
        echo "✅ Usando configuración básica (H2 en memoria)"
        ;;
esac

echo ""
echo "🚀 Iniciando MELIAN MCP Server..."
echo "📝 Para detener el servidor: Ctrl+C"
echo "📖 Documentación completa: ./EJECUTAR_SERVIDOR_MCP.md"
echo ""
echo "=========================================="

# Ejecutar el servidor
java -jar "$JAR_FILE"
