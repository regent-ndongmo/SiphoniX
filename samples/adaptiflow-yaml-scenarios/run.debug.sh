#!/usr/bin/env bash

SCENARIO_FILE="${1:-benin-traffic}"
TOMCAT_DEBUG_PORT="${DEBUG_PORT:-5005}"
TOMCAT_DEBUG_SUSPEND="${DEBUG_SUSPEND:-n}"
SIPHONIX_DEBUG_PORT="${SIPHONIX_DEBUG_PORT:-5006}"
SIPHONIX_DEBUG_SUSPEND="${SIPHONIX_DEBUG_SUSPEND:-n}"

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

if ls ../../adaptiflow-engine-plugin/target/*-jar-with-dependencies.jar >/dev/null 2>&1; then
  mkdir -p ./plugins
  cp ../../adaptiflow-engine-plugin/target/*-jar-with-dependencies.jar ./plugins/adaptiflow-engine-plugin.jar
else
  echo "Missing AdaptiFlow plugin jar. Run 'cd adaptiflow-engine-plugin && mvn clean package' first."
  exit 1
fi

SCENARIO_FILE="${SCENARIO_FILE}" docker compose \
  -f docker-compose.yml \
  -f docker-compose.debug.yml \
  -p siphonix-adaptiflow-yml \
  up -d --build

echo "Started debug sample with scenario: ${SCENARIO_FILE}"
echo "Tomcat JDWP debug port: ${TOMCAT_DEBUG_PORT} (suspend=${TOMCAT_DEBUG_SUSPEND})"
echo "SiphoniX JDWP debug port: ${SIPHONIX_DEBUG_PORT} (suspend=${SIPHONIX_DEBUG_SUSPEND})"
