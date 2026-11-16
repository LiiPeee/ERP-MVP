FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /workspace

# O wrapper valida o hash do Maven sobre o .zip, por isso o unzip é necessário.
RUN apt-get update \
    && apt-get install --yes --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY domain/ domain/
COPY application/ application/
COPY adapter-web/ adapter-web/
COPY adapter-persistence/ adapter-persistence/
COPY adapter-infra/ adapter-infra/
COPY bootstrap/ bootstrap/

# Os testes rodam no CI (precisam de Docker); aqui só empacotamos.
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21.0.13_9-jre-noble
RUN groupadd --system erp && useradd --system --gid erp --no-create-home erp
WORKDIR /app
COPY --from=build /workspace/bootstrap/target/erp-core.jar erp-core.jar
USER erp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/erp-core.jar"]
