# Build Image - referencable by build
FROM maven:3.9-eclipse-temurin-21 as build
WORKDIR /workspace/app

# COPY Module POMs
COPY storage/pom.xml storage/pom.xml
COPY api/pom.xml api/pom.xml

# COPY Parent POM
COPY pom.xml pom.xml

# COPY sources
COPY storage/src storage/src
COPY api/src api/src

# Jar packen
RUN mvn package -DskipTests

# Final image
FROM eclipse-temurin:21-jre
ARG DEPENDENCY=/workspace/app/api/target

# Copy build results
WORKDIR /app
COPY --from=build ${DEPENDENCY}/clevercash-api.jar .

# Specify listening port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "clevercash-api.jar"]
