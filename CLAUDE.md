# CLAUDE.md

Spring Boot 2.x REST microservice demo with Docker, Buildpacks, Kaniko, Skaffold, and K8s deployment.

## Build & Test Commands

```bash
make deps-install       # Install Java 11 + Maven via mise (first run)
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
- **Default branch**: `main`
- **Endpoints**: REST CRUD at `/example/v1/hotels`, Swagger at `/swagger-ui/index.html`, Swagger 2 JSON at `/v2/api-docs`, Actuator at `/actuator/*`
- **Database**: H2 in-memory (JPA/Hibernate)
- **Version manager**: mise (manages Java + Maven)
- **Architecture diagrams**: Mermaid C4Container + sequence block inline in README; validated by `make mermaid-lint` (Docker `minlag/mermaid-cli`, pinned in `Makefile`). Wired into `make static-check`.

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

| Workflow | File | Triggers | Purpose |
|----------|------|----------|---------|
| CI | `ci.yml` | push to main, PRs, `v*` tags, manual dispatch | Test → Integration-test → Build → Docker (scan + smoke + cosign sign + push) → ci-pass aggregator |
| Cleanup | `cleanup-runs.yml` | weekly (Sunday) + manual + `workflow_call` | Delete old workflow runs and caches |

Required secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` (consumed by the `docker` job).

## Upgrade Backlog

Deferred items surfaced by `/upgrade-analysis` 2026-04-22. Review and prune on subsequent runs.

### Wave 3 — Spring Boot 3.x migration (terminal migration for this project)

- [ ] **Spring Boot 2.3.9.RELEASE → 3.x** (EOL since 2022-07). Requires Java 17+. One-step-at-a-time path: 2.3 → 2.7 → 3.x. Resolves ~30 suppressed CVEs in `.trivyignore`.
- [ ] **Springfox 3.0.0 → springdoc-openapi** (Springfox archived since 2020). The `/v3/api-docs` NPE currently worked around via `/v2/api-docs` in tests and README is a known Springfox bug — disappears with migration.
- [ ] **JUnit 4 → JUnit 5** — migrate `HotelControllerTest` (existing) to match the `*IT.java` integration suite. Spring Boot 3 drops JUnit Vintage from default classpath.
- [ ] **XStream → Jackson XML** — pom already pulls `jackson-dataformat-xml`; remove XStream + xmlpull + xpp3_min deps to cut attack surface.
- [ ] **Drop dead MongoDB config** — `spring.data.mongodb.uri` in `application.yml` is unused (no MongoDB dep in pom).
- [ ] **Re-bump `google-java-format` to latest** (1.26+) — currently pinned at 1.19.2 (last Java-11-compatible release); un-pin after Java 17.
- [ ] **Re-run `/architecture-diagrams` review** — Container tech-string `"Spring Boot 2.3.9, Java 11, embedded Tomcat 9"` becomes stale; Renovate cannot update diagram labels.
- [ ] **Review `.trivyignore` entirely** — most entries are Spring Boot 2.3.9 BOM CVEs that disappear once the BOM moves.

### Wave 2 — infrastructure alignment

- [ ] **Dockerfile ARG / Makefile / `.mise.toml` Maven version skew** — Dockerfile `MVN_VERSION=3.9.9`, Makefile `MAVEN_VER=3.9.15`, `.mise.toml` `maven=3.9.15`. Align on one.
- [ ] **Dockerfile ARGs lack `# renovate:` annotations** — `ARG MVN_VERSION`, `ARG JDK_VERSION` are invisible to Renovate.
- [ ] **`scripts/*.sh` hardcoded image tags** — `mongo:4.2.3`, `maven:3-jdk-11`, `paketo-buildpacks/builder:base` are not variables and not Renovate-tracked. Either extract to vars with `# renovate:` comments, or mark the shell scripts as legacy.

### Ongoing

- [ ] **Quarterly `.trivyignore` review** — every suppression has a "resolved when upstream ships fix" clause; entries accumulate otherwise.
- [ ] **Dependabot alert #21** (1 critical, pre-existing) — GitHub flagged on push 2026-04-22; check Settings → Dependabot.
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
