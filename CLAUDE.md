# CLAUDE.md

Spring Boot 2.x REST microservice demo with Docker, Buildpacks, Kaniko, Skaffold, and K8s deployment.

## Build & Test Commands

```bash
make deps-install    # Install Java 11 + Maven via mise (first run)
make build           # Build JAR (skips tests)
make test            # Run unit tests
make run             # Start the application locally
make image-build     # Build Docker image
make ci              # Full local CI pipeline
```

See `make help` for the full target list.

## Project Structure

- `src/main/java/com/test/example/` -- Application source (Spring Boot REST)
- `src/test/java/` -- Tests (MockMvc)
- `pom.xml` -- Maven build config (Spring Boot 2.3.9, Java 11)
- `Dockerfile` / `Dockerfile.maven-host-m2-cache` -- Multi-stage Docker builds
- `scripts/` -- Docker image build scripts (Buildpacks, Kaniko, multi-stage, m2-cache)
- `skaffold.yaml` -- Skaffold config (Paketo buildpacks)
- `hotel.json` -- Sample API payload
- `Makefile` -- Build orchestration
- `.mise.toml` -- Pinned Java/Maven versions
- `renovate.json` -- Dependency update configuration

## Key Details

- **Group/Artifact**: `com.test:spring-boot-demo:1.0.0`
- **Java version**: 11 (Temurin, pinned in `.mise.toml` + `.java-version`)
- **Spring Boot**: 2.3.9.RELEASE (Spring Framework 5.x, embedded Tomcat 9)
- **Default branch**: `master`
- **Endpoints**: REST CRUD at `/example/v1/hotels`, Swagger at `/swagger-ui/index.html`, OpenAPI 3 at `/v3/api-docs`, Actuator at `/actuator/*`
- **Database**: H2 in-memory (JPA/Hibernate)
- **Version manager**: mise (manages Java + Maven)

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

| Workflow | File | Triggers | Purpose |
|----------|------|----------|---------|
| CI | `ci.yml` | push to master, PRs, `v*` tags, manual dispatch | Test → Build → Docker push → ci-pass aggregator |
| Cleanup | `cleanup-runs.yml` | weekly (Sunday) + manual + `workflow_call` | Delete old workflow runs and caches |

Required secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` (consumed by the `docker` job).

## Skills

Use the following skills when working on related files:

| File(s) | Skill |
|---------|-------|
| `Makefile` | `/makefile` |
| `renovate.json` | `/renovate` |
| `README.md` | `/readme` |
| `.github/workflows/*.{yml,yaml}` | `/ci-workflow` |

When spawning subagents, always pass conventions from the respective skill into the agent's prompt.
