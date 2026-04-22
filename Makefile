.DEFAULT_GOAL := help

SHELL := /bin/bash

APP_NAME    := spring-boot-demo
APP_VERSION := $(shell grep -m1 '<version>' pom.xml | sed -n 's:.*<version>\(.*\)</version>.*:\1:p' | head -1)
CURRENTTAG  := $(shell git describe --tags --abbrev=0 2>/dev/null || echo "dev")

# === Tool Versions (pinned) ===
# Java and Maven are pinned in .mise.toml and read natively by mise.
# MAVEN_VER here only backs the deps-maven fallback used inside act/CI containers that lack mise.
# renovate: datasource=maven depName=org.apache.maven:apache-maven
MAVEN_VER := 3.9.15
# renovate: datasource=github-releases depName=google/google-java-format extractVersion=^v(?<version>.*)$
GJF_VERSION := 1.19.2
# renovate: datasource=github-releases depName=hadolint/hadolint
HADOLINT_VERSION := 2.14.0
# renovate: datasource=github-releases depName=nektos/act
ACT_VERSION := 0.2.86
# renovate: datasource=github-releases depName=aquasecurity/trivy
TRIVY_VERSION := 0.58.1
# renovate: datasource=github-releases depName=gitleaks/gitleaks
GITLEAKS_VERSION := 8.22.1

# Ensure tools installed to ~/.local/bin are on PATH for every recipe —
# needed inside the act runner and on fresh shells where rc files aren't sourced.
export PATH := $(HOME)/.local/bin:$(PATH)

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

#deps: @ Verify required tools are installed (java, mvn)
deps:
	@command -v java >/dev/null 2>&1 || { echo "Error: Java 11 required. Run: make deps-install"; exit 1; }
	@command -v $(MVN) >/dev/null 2>&1 || { echo "Error: Maven required. Run: make deps-install"; exit 1; }

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

#deps-hadolint: @ Install hadolint for Dockerfile linting
deps-hadolint:
	@command -v hadolint >/dev/null 2>&1 || { \
		echo "Installing hadolint $(HADOLINT_VERSION)..."; \
		mkdir -p $$HOME/.local/bin; \
		curl -sSfL -o /tmp/hadolint https://github.com/hadolint/hadolint/releases/download/v$(HADOLINT_VERSION)/hadolint-Linux-x86_64; \
		install -m 755 /tmp/hadolint $$HOME/.local/bin/hadolint; \
		rm -f /tmp/hadolint; \
	}

#deps-act: @ Install act for running GitHub Actions locally
deps-act: deps
	@command -v act >/dev/null 2>&1 || { \
		echo "Installing act $(ACT_VERSION)..."; \
		mkdir -p $$HOME/.local/bin; \
		curl -sSfL https://raw.githubusercontent.com/nektos/act/master/install.sh | bash -s -- -b $$HOME/.local/bin v$(ACT_VERSION); \
	}

#deps-trivy: @ Install Trivy for security scanning
deps-trivy:
	@command -v trivy >/dev/null 2>&1 || { \
		echo "Installing trivy $(TRIVY_VERSION)..."; \
		mkdir -p $$HOME/.local/bin; \
		curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b $$HOME/.local/bin v$(TRIVY_VERSION); \
	}

#deps-gitleaks: @ Install gitleaks for secret scanning
deps-gitleaks:
	@command -v gitleaks >/dev/null 2>&1 || { \
		echo "Installing gitleaks $(GITLEAKS_VERSION)..."; \
		mkdir -p $$HOME/.local/bin; \
		curl -sSfL -o /tmp/gitleaks.tar.gz "https://github.com/gitleaks/gitleaks/releases/download/v$(GITLEAKS_VERSION)/gitleaks_$(GITLEAKS_VERSION)_linux_x64.tar.gz"; \
		tar -xzf /tmp/gitleaks.tar.gz -C /tmp gitleaks; \
		install -m 755 /tmp/gitleaks $$HOME/.local/bin/gitleaks; \
		rm -f /tmp/gitleaks /tmp/gitleaks.tar.gz; \
	}

#deps-check: @ Show required tools and installation status
deps-check:
	@echo "--- Tool status ---"
	@for tool in java mvn docker hadolint act trivy gitleaks; do \
		printf "  %-16s " "$$tool:"; \
		command -v $$tool >/dev/null 2>&1 && echo "installed" || echo "NOT installed"; \
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
	@$(MVN) -B spring-boot:run -Drun.arguments="spring.profiles.active=default" -DskipTests

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

#lint: @ Run compiler warnings-as-errors check
lint: deps
	@$(MVN) -B validate
	@$(MVN) -B compile -Dmaven.compiler.failOnWarning=true -q

#lint-docker: @ Lint the Dockerfile with hadolint
lint-docker: deps-hadolint
	@hadolint Dockerfile
	@hadolint Dockerfile.maven-host-m2-cache

#trivy-fs: @ Scan filesystem for vulnerabilities, secrets, and misconfigurations
trivy-fs: deps-trivy
	@trivy fs --scanners vuln,secret,misconfig --severity CRITICAL,HIGH .

#secrets: @ Scan for hardcoded secrets with gitleaks
secrets: deps-gitleaks
	@gitleaks detect --source . --verbose --redact

#cve-check: @ Run OWASP dependency vulnerability scan
cve-check: deps
	@$(MVN) -B org.owasp:dependency-check-maven:check \
		$$([ -n "$$NVD_API_KEY" ] && echo "-DnvdApiKey=$$NVD_API_KEY") \
		-DfailBuildOnCVSS=9

#vulncheck: @ Alias for cve-check
vulncheck: cve-check

#static-check: @ Run all quality and security checks (composite gate)
static-check: format-check lint lint-docker trivy-fs secrets
	@echo "Static check passed."

#integration-test: @ Run integration tests (*IT.java via Failsafe)
integration-test: deps
	@$(MVN) -B verify -P integration-test

#image-build: @ Build Docker image (multi-stage)
image-build: build
	@docker buildx build --load -t $(DOCKER_IMAGE):$(DOCKER_TAG) .

#image-run: @ Run Docker container
image-run: image-stop
	@docker run --rm -d -p 8080:8080 --name $(APP_NAME) $(DOCKER_IMAGE):$(DOCKER_TAG)

#image-stop: @ Stop Docker container
image-stop:
	@docker stop $(APP_NAME) 2>/dev/null || true

#image-push: @ Push Docker image to registry
image-push: image-build
	@docker push $(DOCKER_REGISTRY)/$(DOCKER_REPO):$(DOCKER_TAG)

#ci: @ Full local CI pipeline
ci: deps lint test integration-test build
	@echo "Local CI pipeline passed."

#ci-run: @ Run GitHub Actions workflows locally via act
ci-run: deps-act
	@docker container prune -f 2>/dev/null || true
	@ACT_PORT=$$(shuf -i 40000-59999 -n 1); \
	ARTIFACT_PATH=$$(mktemp -d -t act-artifacts.XXXXXX); \
	act push --container-architecture linux/amd64 \
		--artifact-server-port "$$ACT_PORT" \
		--artifact-server-path "$$ARTIFACT_PATH"

#renovate-validate: @ Validate renovate.json configuration
renovate-validate:
	@command -v npx >/dev/null 2>&1 || { echo "Error: node/npx required for renovate validation"; exit 1; }
	@npx --yes --package renovate -- renovate-config-validator renovate.json

#release: @ Create and push a new tag (interactive)
release:
	@bash -c 'read -p "New tag (current: $(CURRENTTAG)): " newtag && \
		echo "$$newtag" | grep -qE "^v[0-9]+\.[0-9]+\.[0-9]+$$" || { echo "Error: Tag must match vN.N.N"; exit 1; } && \
		echo -n "Create and push $$newtag? [y/N] " && read ans && [ "$${ans:-N}" = y ] && \
		git tag $$newtag && \
		git push origin $$newtag && \
		echo "Done."'

.PHONY: help deps deps-install deps-maven deps-hadolint deps-act deps-trivy deps-gitleaks deps-check \
	clean build test run format format-check lint lint-docker trivy-fs secrets cve-check vulncheck \
	static-check integration-test image-build image-run image-stop image-push ci ci-run \
	renovate-validate release
