#!/usr/bin/env bash
set -euo pipefail

docker compose -f docker-compose.yml -p siphonix-adaptiflow down -v
