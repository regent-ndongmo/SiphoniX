# SiphoniX Docker Sample: AdaptiFlow + JSON Scenarios

This sample runs SiphoniX inside Docker with the AdaptiFlow plugin runtime and loads a scenario from JSON.

Default scenario: `benin-traffic` defined in [scenarios/benin-traffic.json](scenarios/benin-traffic.json).

## Prerequisites

- Docker + Docker Compose
- Build the SiphoniX fat jar first:

```bash
mvn clean package
```

- Build the AdaptiFlow plugin artifact:

```bash
cd adaptiflow-engine-plugin
mvn clean package
cd ..
```

## Run BeninTrafficObservation Scenario

```bash
cd samples/adaptiflow-json-scenarios
chmod +x run.sh stop.sh
./run.sh
```

Or explicitly:

```bash
./run.sh benin-traffic
```

## Run Other Scenarios

```bash
./run.sh cache-size
./run.sh database-availability
```

You can also use `docker compose` directly:

```bash
SCENARIO_FILE=benin-traffic docker compose -f docker-compose.yml -p siphonix-adaptiflow-json up -d --build
SCENARIO_FILE=cache-size docker compose -f docker-compose.yml -p siphonix-adaptiflow-json up -d --build
SCENARIO_FILE=database-availability docker compose -f docker-compose.yml -p siphonix-adaptiflow-json up -d --build
```

## Stop

```bash
./stop.sh
```

## Scenario Files

- [scenarios/benin-traffic.json](scenarios/benin-traffic.json)
- [scenarios/cache-size.json](scenarios/cache-size.json)
- [scenarios/database-availability.json](scenarios/database-availability.json)