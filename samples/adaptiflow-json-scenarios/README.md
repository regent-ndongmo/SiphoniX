# SiphoniX Docker Sample: AdaptiFlow + JSON Scenarios

This sample runs SiphoniX inside Docker with the AdaptiFlow plugin runtime and loads a scenario from JSON.

Default scenario: `benin-traffic` defined in [scenarios/benin-traffic.json](scenarios/benin-traffic.json).

Plugins are loaded from `/opt/siphonix/plugins` inside the container (mapped from `./plugins`).

## Prerequisites

- Docker + Docker Compose
- Build the SiphoniX reactor first. The runtime fat jar is produced in `siphonix-runtime/target`:

```bash
mvn clean package
```

- Build the AdaptiFlow plugin artifact if you want to package only that module:

```bash
cd siphonix-plugins/adaptiflow-engine-plugin
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

## Explore New Plugin Features

List loaded plugins from inside the running manager container:

```bash
docker compose -f docker-compose.yml -p siphonix-adaptiflow-json exec image \
	java -jar /usr/local/tomcat/bin/siphonix.jar \
	--plugin-dir /opt/siphonix/plugins \
	plugin list
```

Check watch mode status:

```bash
docker compose -f docker-compose.yml -p siphonix-adaptiflow-json exec image \
	java -jar /usr/local/tomcat/bin/siphonix.jar \
	--plugin-dir /opt/siphonix/plugins \
	plugin watch status
```

You can place additional plugin jars in `./plugins` and, with `watch-auto`, SiphoniX loads changes automatically.

## Scenario Files

- [scenarios/benin-traffic.json](scenarios/benin-traffic.json)
- [scenarios/cache-size.json](scenarios/cache-size.json)
- [scenarios/database-availability.json](scenarios/database-availability.json)