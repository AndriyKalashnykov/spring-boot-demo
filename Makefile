.DEFAULT_GOAL := help

SHELL := /bin/bash

APP_NAME    := spring-boot-demo
APP_VERSION := $(shell grep -m1 '<version>' pom.xml | sed -n 's:.*<version>\(.*\)</version>.*:\1:p' | head -1)
CURRENTTAG  := $(shell git describe --tags --abbrev=0 2>/dev/null || echo "dev")

# === Tool Versions (pinned) ===
# Java, Maven, hadolint, act, trivy, gitleaks, actionlint, shellcheck,
# container-structure-test are all pinned in .mise.toml. `make deps-install`
# (or `mise install`) provisions them from a single source of truth; CI
# inherits via `jdx/mise-action`. The constants below are kept in the
# Makefile because they cannot live in .mise.toml:
#   - MAVEN_VER: Apache-archives fallback used by `deps-maven` for CI
#     containers that lack mise (kept in sync with .mise.toml's `maven`).
#   - GJF_VERSION: google-java-format ships as a JAR, not a binary —
#     aqua/ubi backends only handle static binaries.
#   - MERMAID_CLI_VERSION: mermaid-cli runs as a Docker image, not a
#     local CLI.
# renovate: datasource=maven depName=org.apache.maven:apache-maven
MAVEN_VER := 3.9.15
# renovate: datasource=github-releases depName=google/google-java-format extractVersion=^v(?<version>.*)$
GJF_VERSION := 1.35.0
# renovate: datasource=docker depName=minlag/mermaid-cli
MERMAID_CLI_VERSION := 11.14.0

# Ensure mise shims and ~/.local/bin are on PATH for every recipe —
# `~/.local/share/mise/shims` is a flat dir of every mise-managed binary
# (works without `eval "$(mise activate bash)"`); `~/.local/bin` covers
# legacy curl installs and the `mise` binary itself on fresh shells.
export PATH := $(HOME)/.local/share/mise/shims:$(HOME)/.local/bin:$(PATH)

# Maven wrapper or system mvn
MVN := mvn

# Docker image config
DOCKER_IMAGE    := $(APP_NAME)
DOCKER_REGISTRY ?= docker.io
DOCKER_REPO     ?= $(APP_NAME)
DOCKER_TAG      ?= $(APP_VERSION)

# google-java-format JAR (cached once)
GJF_JAR := $(HOME)/.cache/google-java-format/google-java-format-$(GJF_VERSION)-all-deps.jar
GJF_URL := https://github.com/google/google-java-format/releases/download/v$(GJF_VERSION)/google-java-format-$(GJF_VERSION)-all-deps.jar

$(GJF_JAR):
	@mkdir -p $(dir $(GJF_JAR))
	@echo "Downloading google-java-format $(GJF_VERSION)..."
	@curl -sSfL -o $(GJF_JAR) $(GJF_URL)

#help: @ List available tasks
help:
	@echo "Usage: make COMMAND"
	@echo "Commands :"
	@grep -E '[a-zA-Z\.\-]+:.*?@ .*$$' $(MAKEFILE_LIST) | tr -d '#' | awk 'BEGIN {FS = ":.*?@ "}; {printf "\033[32m%-24s\033[0m - %s\n", $$1, $$2}'

#deps: @ Verify required tools are installed (java, mvn) and provision mise-managed CLIs
deps:
	@# Auto-install mise-managed tools (hadolint, act, trivy, gitleaks,
	@# actionlint, shellcheck, container-structure-test) when mise is
	@# present locally. CI uses jdx/mise-action so this branch is local-only.
	@if [ -z "$$CI" ] && command -v mise >/dev/null 2>&1; then \
		mise install --yes; \
	fi
	@command -v java >/dev/null 2>&1 || { echo "Error: Java 25 required. Run: make deps-install"; exit 1; }
	@command -v $(MVN) >/dev/null 2>&1 || { echo "Error: Maven required. Run: make deps-install"; exit 1; }

#deps-docker: @ Verify Docker is installed (host requirement, not provisioned by mise)
deps-docker:
	@command -v docker >/dev/null 2>&1 || { echo "Error: Docker required."; exit 1; }

#deps-node: @ Verify Node.js/npx is available (used by renovate-validate)
deps-node:
	@command -v npx >/dev/null 2>&1 || { echo "Error: Node.js/npx required for renovate-validate."; exit 1; }

#deps-install: @ Install Java and Maven via mise (reads .mise.toml)
deps-install:
	@command -v mise >/dev/null 2>&1 || { \
		echo "Installing mise (no root required, installs to ~/.local/bin)..."; \
		curl -fsSL https://mise.run | sh; \
		echo ""; \
		echo "mise installed. Activate it in your shell, then re-run 'make deps-install':"; \
		echo '  bash: echo '\''eval "$$(~/.local/bin/mise activate bash)"'\'' >> ~/.bashrc'; \
		echo '  zsh:  echo '\''eval "$$(~/.local/bin/mise activate zsh)"'\''  >> ~/.zshrc'; \
		exit 0; \
	}
	@mise install

#deps-maven: @ Install Maven from Apache archives (CI container fallback)
deps-maven:
	@command -v $(MVN) >/dev/null 2>&1 || { \
		echo "Installing Maven $(MAVEN_VER)..."; \
		curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$(MAVEN_VER)/binaries/apache-maven-$(MAVEN_VER)-bin.tar.gz" | tar xz -C /opt; \
		ln -sf "/opt/apache-maven-$(MAVEN_VER)/bin/mvn" /usr/local/bin/mvn; \
	}

#deps-check: @ Show required tools and installation status
deps-check:
	@echo "--- Tool status ---"
	@for tool in java mvn docker mise hadolint act trivy gitleaks actionlint shellcheck container-structure-test; do \
		printf "  %-26s " "$$tool:"; \
		command -v $$tool >/dev/null 2>&1 && echo "installed" || echo "NOT installed (run: make deps-install)"; \
	done

#clean: @ Remove build artifacts
clean:
	@$(MVN) -B clean -q

#build: @ Build the JAR (skips tests)
build: deps
	@$(MVN) -B package -DskipTests

#test: @ Run unit tests
test: deps
	@$(MVN) -B test

#run: @ Start the application locally
run: deps
	@$(MVN) -B spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=default" -DskipTests

#format: @ Auto-format Java source code (Google style)
format: $(GJF_JAR)
	@find . -path '*/src/main/java/*.java' -o -path '*/src/test/java/*.java' | \
		xargs java --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
			-jar $(GJF_JAR) --replace

#format-check: @ Verify code formatting (CI gate)
format-check: $(GJF_JAR)
	@find . -path '*/src/main/java/*.java' -o -path '*/src/test/java/*.java' | \
		xargs java --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
			--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
			-jar $(GJF_JAR) --set-exit-if-changed --dry-run > /dev/null

#lint: @ Compiler warnings-as-errors + Checkstyle (google_checks.xml, severity=error)
lint: deps
	@$(MVN) -B validate
	@$(MVN) -B checkstyle:check
	@$(MVN) -B compile -Dmaven.compiler.failOnWarning=true -q

#lint-docker: @ Lint the Dockerfile with hadolint (provisioned by mise)
lint-docker: deps
	@hadolint Dockerfile
	@hadolint Dockerfile.maven-host-m2-cache

#lint-ci: @ Lint GitHub Actions workflows with actionlint (provisioned by mise; uses shellcheck under the hood)
lint-ci: deps
	@actionlint .github/workflows/*.yml

#lint-scripts-exec: @ Verify all shell scripts are committed with the executable bit set
lint-scripts-exec:
	@NONEXEC=$$(find scripts e2e -name '*.sh' -type f -not -executable 2>/dev/null); \
	if [ -n "$$NONEXEC" ]; then \
		echo "Error: shell scripts missing +x (run chmod +x and commit the mode change):"; \
		echo "$$NONEXEC" | sed 's/^/  /'; \
		exit 1; \
	fi

#trivy-fs: @ Scan filesystem for vulnerabilities, secrets, and misconfigurations (provisioned by mise)
trivy-fs: deps
	@trivy fs --scanners vuln,secret,misconfig --severity CRITICAL,HIGH .

#secrets: @ Scan working tree for hardcoded secrets with gitleaks (provisioned by mise)
secrets: deps
	@gitleaks detect --source . --no-git --verbose --redact

#deps-prune: @ List declared but unused / undeclared but used Maven dependencies
deps-prune: deps
	@$(MVN) -B dependency:analyze

#deps-prune-check: @ Fail the build on any used-undeclared or unused-declared Maven dependency
deps-prune-check: deps
	@$(MVN) -B dependency:analyze -DfailOnWarning=true

#cve-check: @ Run OWASP dependency vulnerability scan (CVSS >= 7 blocks; matches the Trivy image scan threshold)
cve-check: deps
	@$(MVN) -B org.owasp:dependency-check-maven:check \
		$$([ -n "$$NVD_API_KEY" ] && echo "-DnvdApiKey=$$NVD_API_KEY") \
		-DfailBuildOnCVSS=7

#vulncheck: @ Alias for cve-check
vulncheck: cve-check

#mermaid-lint: @ Validate Mermaid diagrams in markdown files
mermaid-lint: deps-docker
	@set -euo pipefail; \
	IMG="minlag/mermaid-cli:$(MERMAID_CLI_VERSION)"; \
	if ! docker image inspect "$$IMG" >/dev/null 2>&1; then \
		for attempt in 1 2 3; do \
			if docker pull "$$IMG"; then break; fi; \
			echo "  Pull attempt $$attempt failed; retrying in $$((attempt * 5))s..."; \
			sleep $$((attempt * 5)); \
		done; \
	fi; \
	MD_FILES=$$(grep -lF '```mermaid' README.md CLAUDE.md 2>/dev/null || true); \
	if [ -z "$$MD_FILES" ]; then \
		echo "No Mermaid blocks found — skipping."; \
		exit 0; \
	fi; \
	FAILED=0; \
	for md in $$MD_FILES; do \
		echo "Validating Mermaid blocks in $$md..."; \
		LOG=$$(mktemp); \
		if docker run --rm -v "$$PWD:/data:ro" \
			"$$IMG" \
			-i "/data/$$md" -o "/tmp/$$(basename $$md .md).svg" >"$$LOG" 2>&1; then \
			echo "  PASS: all blocks rendered cleanly."; \
		else \
			echo "  FAIL: parse error in $$md:"; \
			sed 's/^/    /' "$$LOG"; \
			FAILED=$$((FAILED + 1)); \
		fi; \
		rm -f "$$LOG"; \
	done; \
	if [ "$$FAILED" -gt 0 ]; then \
		echo "Mermaid lint: $$FAILED file(s) had parse errors."; \
		exit 1; \
	fi

# `cve-check` is deliberately NOT in `static-check` — the OWASP NVD database
# download dominates runtime (minutes, not seconds) and would slow the
# fast-feedback loop that contributors run on every commit. CVE coverage
# is provided by:
#   * Trivy image scan (CRITICAL/HIGH) in the docker job — every push
#   * `cve-check` CI job — tag pushes + weekly schedule
# Run `make cve-check` locally before tagging a release.
#static-check: @ Run all quality and security checks (composite gate)
static-check: format-check lint lint-docker lint-ci lint-scripts-exec mermaid-lint trivy-fs secrets deps-prune-check
	@echo "Static check passed."

#integration-test: @ Run integration tests (*IT.java via Failsafe)
integration-test: deps
	@$(MVN) -B verify -P integration-test

#e2e: @ Run end-to-end smoke test against the packaged JAR (background process + curl)
e2e: build
	@./e2e/smoke.sh

#image-build: @ Build Docker image (multi-stage)
image-build: build deps-docker
	@docker buildx build --load -t $(DOCKER_IMAGE):$(DOCKER_TAG) .

#image-test: @ Validate image structure (USER, ENTRYPOINT, layout) via container-structure-test
image-test: image-build deps
	@container-structure-test test \
		--image $(DOCKER_IMAGE):$(DOCKER_TAG) \
		--config container-structure-test.yaml

#image-run: @ Run Docker container
image-run: image-build image-stop
	@docker run --rm -d -p 8080:8080 --name $(APP_NAME) $(DOCKER_IMAGE):$(DOCKER_TAG)

#image-stop: @ Stop Docker container
image-stop: deps-docker
	@docker stop $(APP_NAME) 2>/dev/null || true

#image-push: @ Push Docker image to registry
image-push: image-build
	@docker push $(DOCKER_REGISTRY)/$(DOCKER_REPO):$(DOCKER_TAG)

#ci: @ Full local CI pipeline
ci: deps static-check test integration-test build
	@echo "Local CI pipeline passed."

#ci-run: @ Run GitHub Actions workflows locally via act (per-job, fail-fast; act provisioned by mise)
ci-run: deps deps-docker
	@docker container prune -f 2>/dev/null || true
	@# act's synthetic push-event payload omits `repository.default_branch`,
	@# which dorny/paths-filter falls back to when computing the diff base.
	@# Generate a minimal payload via --eventpath; without this, the
	@# `changes` job fails with "This action requires 'base' input or
	@# 'repository.default_branch' to be set in the event payload".
	@evt=$$(mktemp /tmp/act-push-event.XXXXXX.json); \
	printf '{"ref":"refs/heads/main","repository":{"default_branch":"main","name":"$(APP_NAME)","full_name":"AndriyKalashnykov/$(APP_NAME)"}}' >"$$evt"; \
	ACT_PORT=$$(shuf -i 40000-59999 -n 1); \
	ARTIFACT_PATH=$$(mktemp -d -t act-artifacts.XXXXXX); \
	rc=0; \
	for job in static-check test integration-test build e2e; do \
		echo "=== act --job $$job ==="; \
		act push --container-architecture linux/amd64 \
			--artifact-server-port "$$ACT_PORT" \
			--artifact-server-path "$$ARTIFACT_PATH" \
			--eventpath "$$evt" \
			--job "$$job" || { rc=$$?; break; }; \
	done; \
	rm -f "$$evt"; \
	exit $$rc

#renovate-validate: @ Validate renovate.json configuration
renovate-validate: deps-node
	@npx --yes --package renovate -- renovate-config-validator renovate.json

#release: @ Create and push a new tag (interactive)
release:
	@bash -c 'read -p "New tag (current: $(CURRENTTAG)): " newtag && \
		echo "$$newtag" | grep -qE "^v[0-9]+\.[0-9]+\.[0-9]+$$" || { echo "Error: Tag must match vN.N.N"; exit 1; } && \
		echo -n "Create and push $$newtag? [y/N] " && read ans && [ "$${ans:-N}" = y ] && \
		git tag $$newtag && \
		git push origin $$newtag && \
		echo "Done."'

.PHONY: help deps deps-install deps-maven deps-check deps-docker deps-node deps-prune deps-prune-check \
	clean build test run format format-check lint lint-docker lint-ci lint-scripts-exec mermaid-lint trivy-fs secrets \
	cve-check vulncheck static-check integration-test e2e image-build image-test image-run image-stop image-push \
	ci ci-run renovate-validate release
