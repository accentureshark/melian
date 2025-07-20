# Configuración para Claude Desktop

Este archivo muestra cómo conectar el servidor MCP de MELIAN con Claude Desktop.

## Archivo de configuración: claude_desktop_config.json

### Ubicación del archivo de configuración:

**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
**Linux:** `~/.config/Claude/claude_desktop_config.json`

### Contenido del archivo de configuración:

```json
{
  "mcpServers": {
    "melian": {
      "command": "mvn",
      "args": [
        "spring-boot:run",
        "-Dspring.profiles.active=default"
      ],
      "cwd": "/ruta/completa/al/proyecto/melian"
    }
  }
}
```

### Ejemplo con ruta específica:

```json
{
  "mcpServers": {
    "melian-movies": {
      "command": "mvn",
      "args": [
        "spring-boot:run",
        "-Dspring.profiles.active=default",
        "-q"
      ],
      "cwd": "/home/usuario/proyectos/melian",
      "env": {
        "SPRING_PROFILES_ACTIVE": "default"
      }
    }
  }
}
```

### Configuración con script personalizado:

```json
{
  "mcpServers": {
    "melian": {
      "command": "./run-mcp-server.sh",
      "args": ["-q"],
      "cwd": "/ruta/completa/al/proyecto/melian"
    }
  }
}
```

## Pasos para configurar:

1. **Ubicar el archivo de configuración** según tu sistema operativo
2. **Editar o crear** el archivo `claude_desktop_config.json`
3. **Cambiar la ruta** `/ruta/completa/al/proyecto/melian` por la ruta real de tu proyecto
4. **Guardar** el archivo
5. **Reiniciar Claude Desktop** completamente
6. **Verificar** que el servidor aparece en la lista de herramientas de Claude

## Verificar que funciona:

Una vez configurado y reiniciado Claude Desktop, puedes probar con comandos como:

- "¿Qué herramientas tienes disponibles?"
- "Busca películas con título que contenga 'Matrix'"
- "Muéstrame películas populares"
- "Busca películas del género acción"

## Resolución de problemas:

### Error: "Command not found: mvn"
**Solución:** Asegúrate de que Maven esté en el PATH del sistema, o usar ruta completa:
```json
"command": "/usr/local/bin/mvn"
```

### Error: "Java version not supported"
**Solución:** Verificar que Java 17+ esté instalado y configurado en PATH

### Error: "Connection failed"
**Solución:** 
1. Verificar que el proyecto compila: `mvn clean compile`
2. Ejecutar test del servidor: `./test-mcp-server.sh`
3. Verificar logs en Claude Desktop (menú Help > Developer Tools)

### El servidor no aparece en Claude
**Solución:**
1. Verificar que el archivo JSON tiene sintaxis correcta
2. Verificar que la ruta del proyecto es correcta y absoluta
3. Reiniciar Claude Desktop completamente (cerrar y abrir)
4. Revisar los logs en Developer Tools

## Configuración avanzada:

### Con variables de entorno:
```json
{
  "mcpServers": {
    "melian": {
      "command": "mvn",
      "args": ["spring-boot:run"],
      "cwd": "/ruta/al/proyecto/melian",
      "env": {
        "TMDB_ACCESS_TOKEN": "tu_token_aqui",
        "SPRING_DATASOURCE_URL": "jdbc:mysql://localhost:3307/sakila"
      }
    }
  }
}
```

### Con logging habilitado:
```json
{
  "mcpServers": {
    "melian": {
      "command": "mvn",
      "args": [
        "spring-boot:run",
        "-Dlogging.level.org.shark.melian=DEBUG"
      ],
      "cwd": "/ruta/al/proyecto/melian"
    }
  }
}
```

## Nota importante:

El servidor MCP de MELIAN se ejecuta como un proceso hijo de Claude Desktop y se comunica mediante STDIO. No es necesario ejecutar el servidor por separado; Claude Desktop lo iniciará automáticamente cuando sea necesario.