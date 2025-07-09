#!/bin/bash

echo "⏳ Esperando que MongoDB arranque..."
sleep 5

echo "🔎 Verificando si existen documentos..."
count=$(mongosh --quiet --host mongo -u root -p example --authenticationDatabase admin --eval "db.getSiblingDB('movielens').movies.countDocuments()")

if [ "$count" -eq "0" ]; then
  for collection in movies tags links ratings; do
    echo "📥 Importando ${collection}_part_*.csv..."
    for f in ./${collection}_part_*.csv; do
      if [ -f "$f" ]; then
        echo "   ➡️  $f"
        mongoimport --host mongo -u root -p example --authenticationDatabase admin \
          --db movielens --collection $collection \
          --type csv --headerline --file "$f"
      fi
    done
  done
  echo "✅ Importación completada con éxito."
else
  echo "✅ Ya hay datos, no se hace nada."
fi