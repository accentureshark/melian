FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/melian-*.jar app.jar

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]