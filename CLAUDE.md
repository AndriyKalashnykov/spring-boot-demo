# CLAUDE.md

Spring Boot 4 REST microservice demo — hardened container pipeline reference with Docker, Buildpacks, Kaniko, Skaffold, and K8s deployment.

## Build & Test Commands

```bash
make deps-install       # Install Java 25 + Maven via mise (first run)
make build              # Build JAR (skips tests)
make run                # Start the application locally
make image-build        # Build Docker image
make ci                 # Full local CI pipeline
```

Two test layers:

| Target | Layer | Stack | Runtime |
|--------|-------|-------|---------|
| `make test` | Unit | Spring MockMvc, in-process | seconds |
| `make integration-test` | Integration | `*IT.java` via Maven Failsafe, real Tomcat on random port, H2 backend | tens of seconds |

See `make help` for the full target list.

## Project Structure

- `src/main/java/com/test/example/` -- Application source (Spring Boot REST)
- `src/test/java/` -- Tests (MockMvc)
- `pom.xml` -- Maven build config (Spring Boot 4.0.5, Java 25)
- `Dockerfile` / `Dockerfile.maven-host-m2-cache` -- Multi-stage Docker builds
- `scripts/` -- Docker image build scripts (multi-stage, Buildpacks, Kaniko, Spring Boot layered jar, m2-cache) + local run helpers
- `LICENSE` -- MIT
- `skaffold.yaml` -- Skaffold config (Paketo buildpacks)
- `hotel.json` -- Sample API payload
- `Makefile` -- Build orchestration
- `.mise.toml` -- Pinned Java/Maven versions
- `renovate.json` -- Dependency update configuration

## Key Details

- **Group/Artifact**: `com.test:spring-boot-demo:1.0.0`
- **Java version**: 25 (Temurin LTS, pinned in `.mise.toml` + `.java-version`)
- **Spring Boot**: 4.0.5 (Spring Framework 7, embedded Tomcat 11)
- **Default branch**: `main`
- **Endpoints**: REST CRUD at `/example/v1/hotels`, Swagger UI at `/swagger-ui/index.html`, OpenAPI 3 JSON at `/v3/api-docs`, Actuator at `/actuator/*`
- **Database**: H2 in-memory (JPA/Hibernate)
- **Version manager**: mise (manages Java + Maven)
- **Architecture diagrams**: Mermaid C4Container + sequence block inline in README; validated by `make mermaid-lint` (Docker `minlag/mermaid-cli`, pinned in `Makefile`). Wired into `make static-check`.

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

| Workflow | File | Triggers | Purpose |
|----------|------|----------|---------|
| CI | `ci.yml` | push to main, PRs, `v*` tags, manual dispatch | Test → Integration-test → Build → Docker (scan + smoke + cosign sign + push) → ci-pass aggregator |
| Cleanup | `cleanup-runs.yml` | weekly (Sunday) + manual + `workflow_call` | Delete old workflow runs and caches |

No repo-level secrets required — the `docker` job uses `GITHUB_TOKEN` for GHCR auth and Sigstore OIDC for cosign signing. Image is published to `ghcr.io/AndriyKalashnykov/spring-boot-demo/app`.

## Upgrade Backlog

### Done — Spring Boot 4 migration (2026-04-22)

Resolved by the SB 2.3.9 → 4.0.5 migration:

- Spring Boot 2.3.9.RELEASE → 4.0.5, Java 11 → 25 (Temurin LTS)
- Springfox → springdoc-openapi 3
- XStream + xmlpull + xpp3 → Jackson XML (attack surface trimmed, `xmlpull-XmlPullParserFactory` missing-class bug gone)
- javax.* → jakarta.* across all source
- JUnit 4 → JUnit 5 (`HotelControllerTest` rewritten; tests now homogeneous)
- H2 1.4.x → H2 2.x (BOM-managed)
- JsonPath, commons-collections4, Jackson versions now all BOM-managed
- Dead MongoDB config dropped from `application.yml`
- `spring.profiles: test` legacy syntax → `spring.config.activate.on-profile: test`
- Custom `PropertySourcesPlaceholderConfigurer` bean dropped (no longer needed — `CommitInfoController` reads `git.properties` directly, so the `${VAR}` recursion bug stays fixed)
- `.trivyignore` file deleted — 0 CVEs on Spring Boot 4.0.5 image
- `google-java-format` bumped to 1.35.0 (Java 17+ required — now OK with Java 25)
- Runtime base image: `eclipse-temurin:25-jre-jammy`, numeric UID 65532
- `JarLauncher` path updated to `org.springframework.boot.loader.launch.JarLauncher` (relocated in 3.2+)
- `HotelRepository` now extends both `CrudRepository` + `PagingAndSortingRepository` (Spring Data 3 split)
- `HealthIndicator` / `Health` / `Status` moved to `org.springframework.boot.health.contributor`
- `TestRestTemplate` now in its own module `spring-boot-resttestclient` with `@AutoConfigureTestRestTemplate`
- `@DataJpaTest` moved to `spring-boot-data-jpa-test` starter
- `WebConfig` stripped of removed `favorPathExtension` / `useJaf`, no longer has spurious `@EnableWebMvc`

### Deferred

- [ ] **Dockerfile ARGs lack `# renovate:` annotations** — `ARG MVN_VERSION`, `ARG JDK_VERSION` still invisible to Renovate.
- [ ] **`scripts/*.sh` rolling-tag Paketo builder** — `gcr.io/paketo-buildpacks/builder:base` in `build-dockerimage-buildpacks.sh` is a rolling tag (Paketo pushes new versions to `:base` directly). Pin to a specific Paketo builder release if supply-chain provenance matters. (The previously-flagged `mongo:4.2.3` and `maven:3-jdk-11` references were resolved during the SB 4 migration.)
- [ ] **Dependabot alert #21** (1 critical, pre-existing) — check Settings → Dependabot; the SB 4 migration may have resolved it.
- [ ] **`LICENSE` file** — absent; README has no License badge (consistent). Add MIT if publishing the project.
- [ ] **Orphan `master` branch on remote** — stale, `main` is the default. Delete after confirming nothing references it.

## Skills

Use the following skills when working on related files:

| File(s) | Skill |
|---------|-------|
| `Makefile` | `/makefile` |
| `renovate.json` | `/renovate` |
| `README.md` | `/readme` |
| `.github/workflows/*.{yml,yaml}` | `/ci-workflow` |

When spawning subagents, always pass conventions from the respective skill into the agent's prompt.
