# 1) React 빌드
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Spring Boot 빌드 (React 결과물을 static 으로 포함)
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY backend/src ./src
COPY --from=frontend /app/dist ./src/main/resources/static
RUN ./gradlew --no-daemon bootJar -x test

# 3) 실행 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 dlog
COPY --from=backend /app/build/libs/*.jar app.jar
USER dlog
# local 기본값이 적용되지 않도록 운영 프로파일 지정 (환경 변수 누락 시 기동 실패)
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
