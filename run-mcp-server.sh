#!/bin/bash

# Script para ejecutar el servidor MCP de MELIAN
# Uso: ./run-mcp-server.sh [opciones]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar ayuda
show_help() {
    echo "🚀 Script para ejecutar el servidor MCP de MELIAN"
    echo ""
    echo "Uso: $0 [opciones]"
    echo ""
    echo "Opciones:"
    echo "  -h, --help              Mostrar esta ayuda"
    echo "  -c, --compile           Compilar antes de ejecutar"
    echo "  -t, --test              Ejecutar tests antes de ejecutar"
    echo "  -d, --with-databases    Iniciar bases de datos con Docker"
    echo "  -q, --quiet            Modo silencioso"
    echo "  --env-file FILE        Usar archivo de variables de entorno"
    echo ""
    echo "Ejemplos:"
    echo "  $0                      Ejecutar solo con API REST"
    echo "  $0 -d                   Ejecutar con todas las bases de datos"
    echo "  $0 -c -t                Compilar, testear y ejecutar"
    echo "  $0 --env-file .env      Ejecutar con variables de entorno personalizadas"
    echo ""
}

# Variables por defecto
COMPILE=false
RUN_TESTS=false
WITH_DATABASES=false
QUIET=false
ENV_FILE=""

# Procesar argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -c|--compile)
            COMPILE=true
            shift
            ;;
        -t|--test)
            RUN_TESTS=true
            shift
            ;;
        -d|--with-databases)
            WITH_DATABASES=true
            shift
            ;;
        -q|--quiet)
            QUIET=true
            shift
            ;;
        --env-file)
            ENV_FILE="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Error: Opción desconocida $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Función para log con colores
log() {
    if [ "$QUIET" = false ]; then
        echo -e "$1"
    fi
}

# Verificar prerrequisitos
log "${BLUE}🔍 Verificando prerrequisitos...${NC}"

# Verificar Java
if ! command -v java &> /dev/null; then
    log "${RED}❌ Java no está instalado${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    log "${RED}❌ Se requiere Java 17 o superior. Versión actual: $JAVA_VERSION${NC}"
    exit 1
fi
log "${GREEN}✅ Java $JAVA_VERSION detectado${NC}"

# Verificar Maven
if ! command -v mvn &> /dev/null; then
    log "${RED}❌ Maven no está instalado${NC}"
    exit 1
fi
log "${GREEN}✅ Maven detectado${NC}"

# Verificar Docker si se requiere
if [ "$WITH_DATABASES" = true ]; then
    if ! command -v docker &> /dev/null; then
        log "${RED}❌ Docker no está instalado pero se requiere para las bases de datos${NC}"
        exit 1
    fi
    log "${GREEN}✅ Docker detectado${NC}"
fi

# Compilar si se requiere
if [ "$COMPILE" = true ]; then
    log "${BLUE}🔨 Compilando proyecto...${NC}"
    if [ "$QUIET" = true ]; then
        mvn clean compile -q
    else
        mvn clean compile
    fi
    if [ $? -eq 0 ]; then
        log "${GREEN}✅ Compilación exitosa${NC}"
    else
        log "${RED}❌ Error en compilación${NC}"
        exit 1
    fi
fi

# Ejecutar tests si se requiere
if [ "$RUN_TESTS" = true ]; then
    log "${BLUE}🧪 Ejecutando tests...${NC}"
    if [ "$QUIET" = true ]; then
        mvn test -q
    else
        mvn test
    fi
    if [ $? -eq 0 ]; then
        log "${GREEN}✅ Tests exitosos${NC}"
    else
        log "${RED}❌ Error en tests${NC}"
        exit 1
    fi
fi

# Iniciar bases de datos si se requiere
if [ "$WITH_DATABASES" = true ]; then
    log "${BLUE}🗄️  Iniciando bases de datos con Docker...${NC}"
    
    # Verificar si docker-compose.yml existe
    if [ ! -f "docker-compose.yml" ]; then
        log "${RED}❌ Archivo docker-compose.yml no encontrado${NC}"
        exit 1
    fi
    
    docker-compose up -d
    if [ $? -eq 0 ]; then
        log "${GREEN}✅ Bases de datos iniciadas${NC}"
        log "${YELLOW}⏳ Esperando 30 segundos para que las bases de datos estén listas...${NC}"
        sleep 30
    else
        log "${RED}❌ Error iniciando bases de datos${NC}"
        exit 1
    fi
fi

# Preparar comando de ejecución
MAVEN_CMD="mvn spring-boot:run -Dspring.profiles.active=default"

if [ "$QUIET" = true ]; then
    MAVEN_CMD="$MAVEN_CMD -q"
fi

# Usar archivo de entorno si se especifica
if [ -n "$ENV_FILE" ]; then
    if [ -f "$ENV_FILE" ]; then
        log "${BLUE}📁 Cargando variables de entorno desde $ENV_FILE${NC}"
        MAVEN_CMD="env \$(cat $ENV_FILE | xargs) $MAVEN_CMD"
    else
        log "${RED}❌ Archivo de entorno $ENV_FILE no encontrado${NC}"
        exit 1
    fi
fi

# Mostrar información del servidor MCP
log ""
log "${GREEN}🚀 Iniciando servidor MCP de MELIAN${NC}"
log "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log "${YELLOW}📋 Información del servidor:${NC}"
log "   • Nombre: melian-movie-server"
log "   • Versión: 1.0.0"
log "   • Protocolo: MCP (Model Content Protocol)"
log "   • Comunicación: STDIO (JSON-RPC)"
log "   • Puerto web: No aplica (servidor MCP, no HTTP)"
log ""
log "${YELLOW}🔧 Herramientas disponibles:${NC}"
log "   • search_movies_by_title"
log "   • search_movies_by_genre" 
log "   • get_movie_details"
log "   • get_popular_movies"
log "   • search_movies_by_actor"
log ""
log "${YELLOW}📡 Fuentes de datos:${NC}"
if [ "$WITH_DATABASES" = true ]; then
    log "   • MySQL (Sakila) - localhost:3307"
    log "   • MongoDB (MovieLens) - localhost:27017"
fi
log "   • TMDB API REST - api.themoviedb.org"
log ""
log "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log "${GREEN}⚡ El servidor está listo para recibir comandos MCP vía STDIO${NC}"
log "${YELLOW}💡 Para conectar: Configurar cliente MCP para usar este comando${NC}"
log ""

# Ejecutar servidor MCP
eval $MAVEN_CMD