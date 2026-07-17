# SiphoniX Docker Sample: AdaptiFlow + XML Scenarios

This sample runs SiphoniX inside Docker with the AdaptiFlow plugin runtime and loads a scenario from XML.

Default scenario: `benin-traffic` defined in [scenarios/benin-traffic.xml](scenarios/benin-traffic.xml).

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
cd samples/adaptiflow-xml-scenarios
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
SCENARIO_FILE=benin-traffic docker compose -f docker-compose.yml -p siphonix-adaptiflow-xml up -d --build
SCENARIO_FILE=cache-size docker compose -f docker-compose.yml -p siphonix-adaptiflow-xml up -d --build
SCENARIO_FILE=database-availability docker compose -f docker-compose.yml -p siphonix-adaptiflow-xml up -d --build
```

## Stop

```bash
./stop.sh
```

## Scenario Files

- [scenarios/benin-traffic.xml](scenarios/benin-traffic.xml)
- [scenarios/cache-size.xml](scenarios/cache-size.xml)
- [scenarios/database-availability.xml](scenarios/database-availability.xml)

## XML Format

The XML format is centered on a `<scenarios>` root with one or more `<scenario>` entries.

- Scalar parameters can use `<parameter key="name" value="..."/>`
- List parameters use `<parameter key="name"><list>...</list></parameter>`
- Nested lists are supported (for constructor arguments like `[[...], ...]`)