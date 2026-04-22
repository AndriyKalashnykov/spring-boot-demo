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

### Also resolved (2026-04-22 follow-up passes)

- Dockerfile ARGs rewired to full-tag ARGs (`MAVEN_IMAGE_VERSION`, `TEMURIN_IMAGE_VERSION`) with `# renovate: datasource=docker depName=...` comments; Temurin pinned to `25.0.2_10-jre-jammy@sha256:...`
- `scripts/build-dockerimage-buildpacks.sh` rolling `gcr.io/paketo-buildpacks/builder:base` → pinned `paketobuildpacks/builder-jammy-base:0.4.563` with renovate comment + new `scripts/*.sh` custom manager in `renovate.json`
- `BP_OCI_SOURCE` set in pom.xml's `spring-boot-maven-plugin` image config so `mvn spring-boot:build-image` produces a GHCR-auto-linkable image
- Tomcat 11.0.20 → 11.0.21 override (closes CVE-2026-34483 + CVE-2026-34487). Final Trivy: 0 CRITICAL/HIGH on the built image
- Dockerfile + Dockerfile.maven-host-m2-cache: `# syntax=docker/dockerfile:1`, BuildKit `RUN --mount=type=cache,target=/root/.m2`, `COPY --link` on runtime layers, consolidated WORKDIR, hadolint-clean
- `.dockerignore` created (keeps `.git`; excludes everything else)
- LICENSE file (MIT) + License badge added
- `.travis.yml` removed (Travis openjdk11 + Bionic config; CI is GitHub Actions)
- Orphan `master` branch deleted from remote
- Dependabot alert #21 verified fixed (along with 13 other historical alerts; 0 open now)
- ci.yml aligned to `/ci-workflow` canon: `jdx/mise-action` + explicit Maven cache, `contains(needs.*.result, 'failure')` aggregator, `paths-ignore` trigger filter, `workflow_call` trigger, canonical step names on every `uses:` step
- `renovate.json`: Apache commons grouping + `lang: java` label rule added; Jackson rule covers both `com.fasterxml.jackson` and `tools.jackson` namespaces; 7-day throttle on `java-version` datasource for mise-aqua race

## Skills

Use the following skills when working on related files:

| File(s) | Skill |
|---------|-------|
| `Makefile` | `/makefile` |
| `renovate.json` | `/renovate` |
| `README.md` | `/readme` |
| `.github/workflows/*.{yml,yaml}` | `/ci-workflow` |

When spawning subagents, always pass conventions from the respective skill into the agent's prompt.
