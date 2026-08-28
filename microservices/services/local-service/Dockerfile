FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/local-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
