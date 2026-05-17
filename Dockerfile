FROM eclipse-temurin:25-jre

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/todolab.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/todolab.jar"]
