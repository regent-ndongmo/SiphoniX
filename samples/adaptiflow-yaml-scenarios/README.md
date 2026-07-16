# SiphoniX Docker Sample: AdaptiFlow + YAML Scenarios

This sample runs SiphoniX inside Docker with the AdaptiFlow plugin runtime and loads a scenario from YAML.

Default scenario: `benin-traffic` defined in [scenarios/benin-traffic.yml](scenarios/benin-traffic.yml).

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
cd samples/adaptiflow-yaml-scenarios
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
SCENARIO_FILE=benin-traffic docker compose -f docker-compose.yml -p siphonix-adaptiflow up -d --build
SCENARIO_FILE=cache-size docker compose -f docker-compose.yml -p siphonix-adaptiflow up -d --build
SCENARIO_FILE=database-availability docker compose -f docker-compose.yml -p siphonix-adaptiflow up -d --build
```

## Stop

```bash
./stop.sh
```

## Scenario Files

- [scenarios/benin-traffic.yml](scenarios/benin-traffic.yml)
- [scenarios/cache-size.yml](scenarios/cache-size.yml)
- [scenarios/database-availability.yml](scenarios/database-availability.yml)
