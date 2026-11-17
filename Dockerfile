FROM node:26.13.1-bookworm-slim AS frontend
WORKDIR /frontend
ENV NG_CLI_ANALYTICS=false
# O .npmrc bloqueia scripts de instalação de pacotes; npm ci instala exatamente o que está no package-lock.json.
COPY frontend-angular/package.json frontend-angular/package-lock.json frontend-angular/.npmrc ./
RUN npm ci
COPY frontend-angular/ ./
RUN npm run build

FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /workspace

# O wrapper valida o hash do Maven sobre o .zip, por isso o unzip é necessário.
RUN apt-get update \
    && apt-get install --yes --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY backend-java/mvnw backend-java/pom.xml ./
COPY backend-java/.mvn/ .mvn/
COPY backend-java/domain/ domain/
COPY backend-java/application/ application/
COPY backend-java/adapter-web/ adapter-web/
COPY backend-java/adapter-persistence/ adapter-persistence/
COPY backend-java/adapter-infra/ adapter-infra/
COPY backend-java/bootstrap/ bootstrap/

# Os testes rodam no CI (precisam de Docker); aqui só empacotamos.
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21.0.13_9-jre-noble
RUN groupadd --system erp && useradd --system --gid erp --no-create-home erp
WORKDIR /app
COPY --from=build /workspace/bootstrap/target/erp-core.jar erp-core.jar
# O Angular compilado é servido pela própria aplicação (mesma origem: sem CORS e sem recurso de terceiros).
COPY --from=frontend /frontend/dist/frontend/browser /app/static
ENV SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/static/
USER erp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/erp-core.jar"]
