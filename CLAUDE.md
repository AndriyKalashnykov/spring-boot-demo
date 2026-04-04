# CLAUDE.md

Spring Boot 2.x REST microservice demo with Docker, Buildpacks, Kaniko, Skaffold, and K8s deployment.

## Build & Test Commands

```bash
mvn clean package                          # Build JAR
mvn clean package -DskipTests              # Build JAR (skip tests)
mvn -B test --file pom.xml                 # Run tests
mvn clean package spring-boot:run \
  -Drun.arguments="spring.profiles.active=default" -DskipTests  # Run locally
```

## Project Structure

- `src/main/java/com/test/example/` -- Application source (Spring Boot REST)
- `src/test/java/` -- Tests (MockMVC)
- `pom.xml` -- Maven build config (Spring Boot 2.3.9, Java 11)
- `Dockerfile` -- Multi-stage Docker build with BuildKit
- `scripts/` -- Docker image build scripts (Buildpacks, Kaniko)
- `hotel.json` -- Sample API payload

## Key Details

- **Group/Artifact**: `com.test:spring-boot-demo:1.0.0`
- **Java version**: 11 (Temurin)
- **Spring Boot**: 2.3.9.RELEASE
- **Default branch**: `master`
- **Endpoints**: REST CRUD at `/example/v1/hotels`, Swagger at `/swagger-ui/index.html`, Actuator at `/actuator/*`
- **Database**: H2 in-memory (JPA/Hibernate)
- **No Makefile** -- use `mvn` commands directly

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

| Workflow | File | Triggers | Purpose |
|----------|------|----------|---------|
| CI | `main.yml` | push to master | Build Docker image, push to DockerHub |
| Test | `maven-test.yml` | push to master, PRs, `v*` tags | Maven test |
| Cleanup | `cleanup-runs.yml` | weekly (Sunday) + manual | Delete old workflow runs |

## Skills

| File(s) | Skill |
|---------|-------|
| `README.md` | `/readme` |
| `.github/workflows/*.yml` | `/ci-workflow` |
