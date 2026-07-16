#!/usr/bin/env bash

SCENARIO_FILE="${1:-benin-traffic}"

if [[ ! -f "./scenarios/${SCENARIO_FILE}.yml" ]]; then
  echo "Unknown scenario: ${SCENARIO_FILE}"
  echo "Available scenarios:"
  ls -1 ./scenarios | sed 's/\.yml$//'
  exit 1
fi

if ls ../../target/*-jar-with-dependencies.jar >/dev/null 2>&1; then
  cp ../../target/*-jar-with-dependencies.jar ./siphonix-app.jar
else
  echo "Missing fat jar. Run 'mvn clean package' at repository root first."
  exit 1
fi

SCENARIO_FILE="${SCENARIO_FILE}" docker compose -f docker-compose.yml -p siphonix-adaptiflow up -d --build

echo "Started sample with scenario: ${SCENARIO_FILE}"
