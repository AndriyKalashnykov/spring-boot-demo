#!/bin/bash
# Build the image via Cloud Native Buildpacks (pack CLI) against the pinned
# Paketo jammy-base builder. Install pack: https://buildpacks.io/docs/tools/pack/cli/install/
#   brew install buildpacks/tap/pack
#
# References:
#   https://github.com/paketo-buildpacks/spring-boot
#   https://github.com/spring-cloud/spring-cloud-bindings

set -euo pipefail

# renovate: datasource=docker depName=paketobuildpacks/builder-jammy-base
PAKETO_BUILDER_VERSION="0.4.563"
PAKETO_BUILDER="paketobuildpacks/builder-jammy-base:${PAKETO_BUILDER_VERSION}"

LAUNCH_DIR=$(pwd)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

docker image rm -f spring-boot-demo:latest || true

pack build spring-boot-demo --builder="$PAKETO_BUILDER" --path=.

docker run --rm --name spring-boot-demo -p 8080:8080 -p 8181:8081 spring-boot-demo:latest

cd "$LAUNCH_DIR"
