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

Three test layers:

| Target | Layer | Stack | Runtime |
|--------|-------|-------|---------|
| `make test` | Unit | Spring MockMvc, in-process | seconds |
| `make integration-test` | Integration | `*IT.java` via Maven Failsafe, real Tomcat on random port, H2 backend | tens of seconds |
| `make e2e` | E2E | Boots the packaged JAR on a kernel-allocated free port, curl-based CRUD + Actuator + Swagger smoke (`e2e/smoke.sh`) | ~10 seconds |

See `make help` for the full target list.

## Project Structure

- `src/main/java/com/test/example/` -- Application source (Spring Boot REST)
- `src/test/java/` -- Tests: `.../test/` (MockMvc unit), `.../it/` (`*IT.java` Failsafe integration)
- `pom.xml` -- Maven build config (Spring Boot 4.1.0, Java 25)
- `Dockerfile` / `Dockerfile.maven-host-m2-cache` -- Multi-stage Docker builds
- `container-structure-test.yaml` -- USER/ENTRYPOINT/layered-jar layout assertions (run via `make image-test`)
- `e2e/smoke.sh` -- End-to-end smoke test (boots the packaged JAR, curl-based CRUD + Actuator + Swagger; run via `make e2e`)
- `scripts/` -- Docker image build scripts (multi-stage, Buildpacks, Kaniko, Spring Boot layered jar, m2-cache) + local run helpers
- `LICENSE` -- MIT
- `skaffold.yaml` -- Skaffold config (Paketo buildpacks)
- `scripts/hotel.json` / `scripts/hotel.xml` -- Sample API payloads (JSON + XML)
- `Makefile` -- Build orchestration
- `.mise.toml` -- Pinned Java/Maven versions
- `renovate.json` -- Dependency update configuration
- `img/` -- README image assets (IntelliJ remote-run screenshot)

## Key Details

- **Group/Artifact**: `com.test:spring-boot-demo:0.0.1`
- **Java version**: 25 (Temurin LTS, pinned in `.mise.toml` + `.java-version`)
- **Spring Boot**: 4.1.0 (Spring Framework 7, embedded Tomcat 11)
- **Default branch**: `main`
- **Endpoints**: REST CRUD at `/example/v1/hotels`, Swagger UI at `/swagger-ui/index.html`, OpenAPI 3 JSON at `/v3/api-docs`, Actuator at `/actuator/*`
- **Database**: H2 in-memory (JPA/Hibernate)
- **Version manager**: mise (manages Java + Maven)
- **Architecture diagrams**: Mermaid C4Context (hero) + C4Container + sequence block inline in README; validated by `make mermaid-lint` (Docker `minlag/mermaid-cli`, pinned in `Makefile`). Wired into `make static-check`.

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

| Workflow | File | Triggers | Purpose |
|----------|------|----------|---------|
| CI | `ci.yml` | push to main, PRs, `v*` tags, manual dispatch, weekly schedule (`cve-check` only) | changes (paths-filter) → static-check → (test, integration-test, build) → (e2e, docker = scan + smoke + container-structure-test + tag-gated `linux/amd64` push + sign) + cve-check (tags + weekly) → ci-pass aggregator |
| Cleanup old workflow runs | `cleanup-runs.yml` | weekly (Sunday) + manual + `workflow_call` | Delete old workflow runs and orphaned caches (preserves `main`, default branch, tags) |

The `cve-check` job runs OWASP dependency-check against two independent vuln sources, each gated on optional secrets:
- `NVD_API_KEY` (free key from NIST NVD; without it the NVD analyzer still runs but throttled).
- `OSS_INDEX_USER` + `OSS_INDEX_TOKEN` (free token from https://ossindex.sonatype.org/) — the Sonatype OSS Index analyzer now mandates token auth and is **silently disabled** (warning only, exit 0) without them, so a green `cve-check` without these runs at reduced coverage. Both tokens are routed through a transient `0600` `settings.xml` (never on mvn's argv) referenced by `-DnvdApiServerId`/`-DossIndexServerId`.

The `docker` job uses `GITHUB_TOKEN` for GHCR auth and Sigstore OIDC for cosign signing. Image is published to `ghcr.io/andriykalashnykov/spring-boot-demo/app` (OCI refs are lowercase).

### Historical secret leaks (rotated; informational)

Two pre-Spring-Boot-4 commits introduced secrets that were later removed from the working tree but remain in git history. `make secrets` uses `gitleaks --no-git` (working-tree only) so they do not re-fire, but anyone running a full-history scan (`gitleaks detect --source .` without `--no-git`) will see them. Verify these credentials have been rotated; do not reintroduce.

| Fingerprint | Commit date | File | Rule |
|-------------|-------------|------|------|
| `bededf26e6cddae9d3baf6b56ecd989831dd233d:config.json:generic-api-key:1` | 2020-08-22 | `config.json` (removed) | generic-api-key |
| `d0585218c825162913ba1c895a901143eeb32ff6:plain.cfg:private-key:19` | 2020-09-24 | `plain.cfg` (removed) | private-key |

## Migration History

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

### Also resolved (2026-05-22 — project-review pass)

- Tomcat `11.0.21` → `11.0.22` override — 6 newly-disclosed CVEs (CVE-2026-41293/43512/43515 CRITICAL, CVE-2026-41284/42498/43513 HIGH); rebuilt image scans 0 CRITICAL/HIGH
- `cve-check` no longer passes the NVD key on the `mvn` command line — routed through a transient `settings.xml` + `-DnvdApiServerId` (no `ps`/`/proc` argv leak)
- `pom.xml` `maven-compiler-plugin` now sets `failOnWarning` so every `mvn` build (not only `make lint`) treats compiler warnings as errors
- `ci.yml`: tag pushes force `changes.code=true` (closes the empty-diff silent-no-op-release trap); heavy-job `if:` clauses gained the `!failure() && !cancelled()` guard; Maven cache keys gained a `runner.os` prefix
- `renovate.json`: `skaffold.yaml` buildpacks builder now Renovate-tracked; Makefile customManager regex covers `?=` / `registryUrl`; Maven distribution grouped across `mise` + `Makefile`; `pinDigests` disabled for Makefile docker pins
- `scripts/build-dockerimage-kaniko.sh`: docker-auth `config.json` written `0600` and removed on any exit

### Also resolved (2026-05-22 — project-review follow-up)

- Removed dead `src/main/fabric8/` manifests — orphaned pre-Spring-Boot-4 fabric8-maven-plugin scaffolding (no plugin generated or consumed them; never-filtered `${project.*}` placeholders, JDK 8/9 JVM flags, non-existent image ref, probes pointing at `/health:8081` on an app that serves `/actuator/health:8080`).
- Bean Validation on the REST surface: `@NotBlank` on `Hotel.name`, `@Valid` on POST/PUT request bodies, `@Validated` on `HotelController` plus `@Min(0)` / `@Min(1)` on `page` / `size`. Bad client input now returns **400** instead of **500** (POST with null/blank name, GET `?page=-1`, GET `?size=0`).
- `AbstractRestHandler`: new `@ExceptionHandler` mappings for `ConstraintViolationException` and `IllegalArgumentException` → 400. Spring's default resolver does not auto-map either; the violation / IAE previously propagated as 500.
- `pom.xml`: added the `spring-boot-maven-plugin` `build-info` goal execution so `/actuator/info` exposes `build.artifact/version/group` via the default-enabled `BuildInfoContributor`. Since Spring Boot 2.6+, `EnvironmentInfoContributor` (the path the existing `info.build.*` config relied on) is opt-in via `management.info.env.enabled`.
- `skaffold.yaml`: migrated `apiVersion: skaffold/v2beta8` → `skaffold/v4beta13` via `skaffold fix` (skaffold v2.17.1); manually restored the Renovate `# renovate:` annotation that `skaffold fix` stripped.
- Test coverage: added `HotelControllerIT.createWithNullNameReturns400` + `createWithBlankNameReturns400` + `getHotelsNegativePageReturns400` + `getHotelsZeroSizeReturns400`, `HotelRepositoryIT.findHotelByCityNoMatchReturnsNull`; deepened `ActuatorIT.infoEndpointExposesBuildArtifact` (asserts `build.artifact = "spring-boot-demo"`) and `ActuatorIT.prometheusScrapeReturnsExpositionFormat` (asserts `# HELP` + `# TYPE`).
- Confirmed not actionable: the Mermaid C4 "add a legend" LOW finding — Mermaid's C4 implementation has no legend directive (`[ ] Legend` is explicitly listed as an unimplemented feature in the official Mermaid docs). Resolving would require migrating diagrams from Mermaid C4 to C4-PlantUML, far beyond a LOW.

### Also resolved (2026-06-06 — upgrade-analysis)

- Removed the stale `<jackson-bom.version>3.1.1</jackson-bom.version>` override from `pom.xml`. It was added to fix GHSA-2m67-wjpj-xhg9 (Jackson Core document-length bypass, first patched 3.1.1) back when the Spring Boot **4.0.5** BOM pinned Jackson 3.1.0. The **4.0.6** BOM now ships `jackson-bom.version = 3.1.2`, which already includes that fix — so the override had become redundant *and* a downgrade (it pinned `tools.jackson.core:*` to 3.1.1, below the BOM's 3.1.2). Removing it lets the project ride the BOM; `dependency:tree` confirms `tools.jackson.core:jackson-core/jackson-databind` now resolve to 3.1.2. Verified: unit 2/2 + integration 32/32 green. Not Renovate-tracked (parent-property override not referenced by a declared dependency), so removal — not tracking — was the fix.
- Kept the Tomcat `11.0.22` override (verified still required): all 6 CVEs in its comment are first-patched in 11.0.22, while the 4.0.6 BOM still ships the vulnerable 11.0.21 (range `>= 11.0.0-M1, < 11.0.22`). 11.0.22 is also the latest 11.0.x patch.

### Also resolved (2026-06-13 — unblock Renovate PR backlog)

Three open Renovate PRs were all BLOCKED; root-caused and resolved together:

- **Dockerfile base-image CVE hardening (the real blocker for #114 + #115).** The `docker` job's Trivy image scan (`ignore-unfixed: true`, CRITICAL/HIGH blocks) failed on `libssl3`/`openssl` **CVE-2026-45447** (Heap UAF in `PKCS7_verify`, HIGH): the `eclipse-temurin:25-jre-jammy` base ships stale `3.0.2-0ubuntu1.23`; fixed in `3.0.2-0ubuntu1.25` (published in jammy-security + jammy-updates 2026-06-09). Added `apt-get update && apt-get upgrade -y && apt-get clean && rm -rf /var/lib/apt/lists/*` to the runtime stage of **both** `Dockerfile` and `Dockerfile.maven-host-m2-cache` (as root, before the `USER 65532` switch). Empirically verified: base scans **2 HIGH → 0 HIGH/0 CRITICAL** after the upgrade. No stray non-apt CVE binaries on this base (`/usr/bin/pebble` absent — the portfolio pebble-removal step is N/A). hadolint-clean (DL3005 targets `dist-upgrade`, not `upgrade`). This is a **`main`** fix — both #114 and #115 failed identically on it while their own diffs were clean (per the "N PRs failing identically → diagnose main" triage rule).
- **Spring Boot `4.0.6` → `4.1.0` (PR #115).** 4.1.0 confirmed GA on Maven Central (released 2026-06-10; final, not RC/M). No breaking change touches the application code (no deferred/lazy JPA bootstrap-mode, no AOT/native build, servlet MVC only). Spring Framework rides `7.0.7 → 7.0.8` (patch, not 7.1). Verified: unit 2/2 + integration 32/32 green.
- **Dockerfile jarmode migration `layertools` → `tools` (4.1.0 breaking change, BLOCKING).** Spring Boot 4.x **removed** the `layertools` jarmode; the build stage of both Dockerfiles used `java -Djarmode=layertools -jar ./*.jar extract`, which on 4.1.0 fails the image build with `Error: Unsupported jarmode 'layertools'`. (This is why PR #115's `docker` job failed at the *build* step, not the scan — a distinct cause from #114's libssl3 scan failure.) Migrated to `java -Djarmode=tools -jar ./*.jar extract --layers --launcher --destination extracted`, which reproduces the identical `dependencies/snapshot-dependencies/spring-boot-loader/application` layout (verified empirically — `JarLauncher.class` present), so the `COPY` layers (now from `extracted/`) and the `JarLauncher` ENTRYPOINT are unchanged. Validated by a full local `docker build` + boot smoke test before pushing.
- **Removed the now-redundant `<tomcat.version>11.0.22</tomcat.version>` override.** The 4.1.0 BOM already ships Tomcat **11.0.22** (verified via `help:evaluate` → `11.0.22`), so the override matched the BOM exactly — same hygiene as the 2026-06-06 jackson-bom removal. (It had to stay while on 4.0.6, whose BOM shipped the vulnerable 11.0.21; only removable once on 4.1.0.) Not Renovate-tracked, so removal — not tracking — was the fix.
- **Paketo builder `0.4.586` → `0.4.589` (PR #114)** in `scripts/build-dockerimage-buildpacks.sh` + `skaffold.yaml`. Valid bump; buildpacks path is not scanned by the CI Trivy gate (it scans only the `Dockerfile`-built `spring-boot-demo:scan`).
- **Closed PR #113 (trivy `0.71.0` → `0.71.1` in `.mise.toml`) as un-mergeable.** Root cause: trivy created git tag `v0.71.1` (2026-06-10) but **published no GitHub release / binary assets** for it (the `latest` release is still `v0.71.0`), so mise's aqua backend 404s on `releases/tags/v0.71.1` and `static-check` can't install the tool. Not a config defect — the version is genuinely uninstallable. Renovate re-proposes when a real release lands.

## Upgrade Backlog

- _(empty — Spring Boot is on 4.1.0, the latest GA as of 2026-06-13. Renovate proposes the next bump when it lands.)_

## Skills

Use the following skills when working on related files:

| File(s) | Skill |
|---------|-------|
| `Makefile` | `/makefile` |
| `renovate.json` | `/renovate` |
| `README.md` | `/readme` |
| `.github/workflows/*.{yml,yaml}` | `/ci-workflow` |

When spawning subagents, always pass conventions from the respective skill into the agent's prompt.
