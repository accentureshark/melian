#!/bin/bash
set -e  # Detiene el script si hay errores

echo "⏳ Esperando que MongoDB arranque..."
sleep 10  # Aumentamos el tiempo de espera para asegurar que MongoDB esté listo

echo "🔎 Verificando si existen documentos importados..."
# Mostramos exactamente cuántos documentos hay, no solo si hay más de 2
doc_count=$(mongosh --quiet --host localhost -u root -p example --authenticationDatabase admin --eval "db.getSiblingDB('melian_movies').movies.countDocuments()" || echo "0")
echo "Documentos actuales en movies: $doc_count"

# Verificar detalladamente el archivo CSV
echo "🔍 Verificando archivo CSV:"
if [ -f "/docker-entrypoint-initdb.d/movies.csv" ]; then
  echo "✓ Archivo movies.csv encontrado"
  ls -la /docker-entrypoint-initdb.d/movies.csv
  echo "Primeras líneas del CSV (para verificar formato):"
  head -n 3 /docker-entrypoint-initdb.d/movies.csv
else
  echo "❌ No se encontró el archivo movies.csv"
fi

# Importamos si el conteo es exactamente 2 (los documentos de ejemplo)
if [ "$doc_count" -eq "2" ]; then
  echo "📥 Se detectaron solo los 2 documentos de ejemplo. Intentando importar movies.csv..."

  if [ -f "/docker-entrypoint-initdb.d/movies.csv" ]; then
    # Importación con modo verbose para ver más detalles
    echo "Ejecutando importación..."
    # Reemplaza la línea de mongoimport actual por esta:
    mongoimport --host localhost -u root -p example --authenticationDatabase admin \
      --db melian_movies --collection movies \
      --type csv --headerline \
      --mode insert \
      --file "/docker-entrypoint-initdb.d/movies.csv" \
      --ignoreBlanks \
      --verbose

    echo "✓ Comando de importación completado"

    # Verificamos nuevamente el conteo después de importar
    new_count=$(mongosh --quiet --host localhost -u root -p example --authenticationDatabase admin --eval "db.getSiblingDB('melian_movies').movies.countDocuments()" || echo "0")
    echo "📊 Documentos en movies después de importar: $new_count"

    if [ "$new_count" -gt "2" ]; then
      echo "✅ Importación exitosa: se agregaron $(($new_count - 2)) documentos"
    else
      echo "❌ La importación parece haber fallado. No se agregaron nuevos documentos."
    fi
  fi

  # Registramos la importación
  mongosh --quiet --host localhost -u root -p example --authenticationDatabase admin --eval "db.getSiblingDB('melian_movies').csv_imports.insertOne({completed: true, date: new Date()})"
else
  echo "ℹ️ Ya hay $doc_count documentos en la colección movies. No se hace nada."
fi