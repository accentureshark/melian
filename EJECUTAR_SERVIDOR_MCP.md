# 🚀 Cómo Ejecutar el Servidor MCP de MELIAN

## ¿Qué es el Servidor MCP?

Después del refactor, MELIAN ahora es un **servidor MCP (Model Context Protocol) compliant** que utiliza el SDK oficial de Java. Esto significa que:

- ✅ **Sin Spring Boot**: Aplicación Java pura más liviana y rápida
- ✅ **Protocolo MCP Estándar**: Compatible con cualquier cliente MCP
- ✅ **Comunicación STDIO**: Usa STDIN/STDOUT para comunicación MCP
- ✅ **Menor overhead**: Startup más rápido, menor uso de memoria

## 📋 Requisitos Previos

### Obligatorios:
- **Java 17+** (verificar con `java --version`)
- **Maven 3.8+** (solo para compilación)

### Opcionales (según configuración):
- **Docker** (para MongoDB)
- **MySQL** (si usas base de datos externa)
- **Token TMDB** (para búsquedas de películas reales)

## 🛠️ Compilación

```bash
# Limpiar y compilar el proyecto
mvn clean package -DskipTests

# Verificar que se creó el JAR
ls -la target/melian-*.jar
```

## 🏃‍♂️ Ejecución Rápida (Configuración Básica)

### 1. Ejecución con configuración mínima (H2 en memoria):

```bash
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

Esta configuración usa:
- Base de datos H2 en memoria (sin persistencia)
- Sin MongoDB
- Sin token TMDB (búsquedas limitadas)

### 2. Ver los logs del servidor:

Al ejecutar verás algo como:
```
INFO org.shark.melian.MelianMcpServer -- MELIAN MCP Server initialized successfully
INFO org.shark.melian.MelianMcpServer -- Starting MELIAN MCP Server...
INFO org.shark.melian.MelianMcpServer -- MELIAN MCP Server started with STDIO transport
INFO org.shark.melian.MelianMcpServer -- Server is ready to accept MCP connections via STDIO...
```

## ⚙️ Configuración Completa con Variables de Entorno

### 1. Con Token TMDB (Recomendado):

```bash
# Configurar token TMDB para búsquedas reales
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"

# Ejecutar el servidor
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

### 2. Con MySQL:

```bash
# Configurar base de datos MySQL
export DB_URL="jdbc:mysql://localhost:3306/melian"
export DB_USERNAME="tu_usuario"
export DB_PASSWORD="tu_contraseña"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"

# Token TMDB
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"

# Ejecutar el servidor
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

### 3. Con MongoDB:

```bash
# Primero levantar MongoDB con Docker
docker-compose -f mongodb-docker-compose.yml up -d

# Configurar MongoDB
export MONGODB_URI="mongodb://root:example@localhost:27017"
export MONGODB_DATABASE="melian"

# Token TMDB
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"

# Ejecutar el servidor
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

### 4. Configuración Completa (MySQL + MongoDB + TMDB):

```bash
# Levantar base de datos
docker-compose up -d

# Configurar todas las variables
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"
export DB_URL="jdbc:mysql://localhost:3307/sakila"
export DB_USERNAME="sakila"
export DB_PASSWORD="sakila"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
export MONGODB_URI="mongodb://root:example@localhost:27017"
export MONGODB_DATABASE="melian"

# Ejecutar el servidor
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

## 🐳 Ejecución con Docker

### 1. Levantar bases de datos:
```bash
# Solo MongoDB
docker-compose -f mongodb-docker-compose.yml up -d

# O bases de datos completas (MySQL + MongoDB)
docker-compose up -d
```

### 2. Verificar que están corriendo:
```bash
docker ps
```

### 3. Ejecutar el servidor con configuración:
```bash
export TMDB_ACCESS_TOKEN="tu_token_tmdb_aqui"
java -jar target/melian-0.1.0-SNAPSHOT.jar
```

## 🔧 Obtener Token TMDB

1. Ir a [https://www.themoviedb.org/](https://www.themoviedb.org/)
2. Crear cuenta gratuita
3. Ir a Settings → API
4. Solicitar API Key
5. Usar el "Read Access Token" (formato Bearer)

## 📊 Verificación del Servidor

### 1. El servidor debe mostrar:
```
INFO org.shark.melian.MelianMcpServer -- Server is ready to accept MCP connections via STDIO...
```

### 2. Verificar conexiones:
- **H2**: Siempre funciona (en memoria)
- **MySQL**: Sin errores de conexión si está configurado
- **MongoDB**: Sin errores de "Connection refused" si está corriendo
- **TMDB**: Sin errores de autenticación si el token es válido

## 🛑 Detener el Servidor

```bash
# Ctrl+C en la terminal donde corre el servidor
# O desde otra terminal:
pkill -f "melian-0.1.0-SNAPSHOT.jar"
```

## ❗ Troubleshooting

### Error: MongoDB Connection Refused
```bash
# Verificar que MongoDB está corriendo
docker ps | grep mongo

# Si no está, levantarlo:
docker-compose -f mongodb-docker-compose.yml up -d
```

### Error: MySQL Connection Failed
```bash
# Verificar que MySQL está corriendo
docker ps | grep mysql

# Si no está, levantarlo:
docker-compose up -d mysql-sakila
```

### Error: TMDB Unauthorized
```bash
# Verificar tu token TMDB
echo $TMDB_ACCESS_TOKEN

# Debe empezar con "eyJ..." y ser válido
```

### Server No Responde
```bash
# Verificar que Java 17+ está instalado
java --version

# Verificar que el JAR existe
ls -la target/melian-*.jar

# Recompilar si es necesario
mvn clean package -DskipTests
```

## 🎯 Próximos Pasos

Una vez que el servidor esté corriendo:

1. **Cliente MCP**: Conectar tu cliente MCP preferido
2. **Herramientas Disponibles**: El servidor expone herramientas MCP para buscar películas
3. **Recursos Disponibles**: Acceso a metadata y chunks para sistemas RAG
4. **Integración**: Usar con EvolvAI u otros clientes MCP

## 📚 Más Información

- Ver `README.md` para arquitectura general
- Ver `MCP.md` para detalles del protocolo MCP
- Ver `INTEGRATION_TESTS.md` para tests de integración
- Revisar logs del servidor para debug