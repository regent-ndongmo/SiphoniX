#!/usr/bin/env bash

SCENARIO_FILE="${1:-benin-traffic}"
TOMCAT_DEBUG_PORT="${DEBUG_PORT:-5005}"
TOMCAT_DEBUG_SUSPEND="${DEBUG_SUSPEND:-n}"
SIPHONIX_DEBUG_PORT="${SIPHONIX_DEBUG_PORT:-5006}"
SIPHONIX_DEBUG_SUSPEND="${SIPHONIX_DEBUG_SUSPEND:-n}"

if [[ ! -f "./scenarios/${SCENARIO_FILE}.xml" ]]; then
  echo "Unknown scenario: ${SCENARIO_FILE}"
  echo "Available scenarios:"
  ls -1 ./scenarios | sed 's/\.xml$//'
  exit 1
fi

if ls ../../siphonix-runtime/target/*-jar-with-dependencies.jar >/dev/null 2>&1; then
  cp ../../siphonix-runtime/target/*-jar-with-dependencies.jar ./siphonix-app.jar
else
  echo "Missing runtime fat jar. Run 'mvn clean package' at repository root first."
  exit 1
fi

if ls ../../siphonix-plugins/adaptiflow-engine-plugin/target/*-jar-with-dependencies.jar >/dev/null 2>&1; then
  mkdir -p ./plugins
  cp ../../siphonix-plugins/adaptiflow-engine-plugin/target/*-jar-with-dependencies.jar ./plugins/adaptiflow-engine-plugin.jar
else
  echo "Missing AdaptiFlow plugin jar. Run 'mvn clean package' at repository root first."
  exit 1
fi

SCENARIO_FILE="${SCENARIO_FILE}" docker compose \
  -f docker-compose.yml \
  -f docker-compose.debug.yml \
  -p siphonix-adaptiflow-xml \
  up -d --build

echo "Started debug sample with scenario: ${SCENARIO_FILE}"
echo "Tomcat JDWP debug port: ${TOMCAT_DEBUG_PORT} (suspend=${TOMCAT_DEBUG_SUSPEND})"
echo "SiphoniX JDWP debug port: ${SIPHONIX_DEBUG_PORT} (suspend=${SIPHONIX_DEBUG_SUSPEND})"