#!/bin/bash
# Build a container image via `mvn spring-boot:build-image` (Cloud Native
# Buildpacks under the hood). The image name comes from the pom.xml
# <docker.image.name> property — defaults to spring-boot-demo:1.0.0.
# Requires Docker to be installed.

set -euo pipefail

LAUNCH_DIR=$(pwd)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

mvn clean spring-boot:build-image -DskipTests

docker rm -f spring-boot-demo 2>/dev/null || true
docker run --rm --name spring-boot-demo -p 8080:8080 -p 8181:8081 spring-boot-demo:1.0.0

cd "$LAUNCH_DIR"
