# SiphoniX

SiphoniX is a modular autonomic management runtime for microservice adaptation.

This repository now includes a folder-driven plugin runtime with configurable discovery behavior, plugin lifecycle commands, and scenario-source loading delegated to plugins.

## Current Runtime State

### Startup and Plugin Discovery

SiphoniX supports plugin discovery from a configurable folder.

Configuration precedence:
1. CLI argument `--plugin-dir`
2. Environment variable `SIPHONIX_PLUGIN_DIR`
3. Default `/opt/siphonix/plugins`

Discovery mode precedence:
1. CLI argument `--plugin-discovery-mode`
2. Environment variable `SIPHONIX_PLUGIN_DISCOVERY_MODE`
3. Default `startup-only`

Supported discovery modes:
1. `startup-only`: load plugins at startup only.
2. `watch-auto`: continuously scan and automatically load/replace/remove plugins.
3. `watch-manual`: detect artifacts and queue them for explicit activation.

Backward compatibility:
1. `SIPHONIX_PLUGIN_ARTIFACT` is still accepted for single-artifact loading.

### Plugin Loader Behavior

The runtime loader:
1. Scans plugin directory for `.jar` files.
2. Uses `ServiceLoader` with per-artifact classloaders.
3. Resolves plugin type from known contracts (currently includes `ScenarioManagementPlugin`).
4. Registers/replaces plugins in `PluginRegistry`.
5. Closes previous classloaders on replacement.

### Scenario Loading Responsibility

SiphoniX is format-agnostic and passes path-based scenario sources.

Plugin implementations resolve parsing format (yaml/json/xml) internally.

### Plugin Commands

SiphoniX command mode supports:
1. `plugin list`
2. `plugin load <jarPath>`
3. `plugin unload <pluginId>`
4. `plugin start <pluginId>`
5. `plugin stop <pluginId>`
6. `plugin reload <pluginId>`
7. `plugin watch on|off|status`

## Usage

### Daemon Mode

```bash
java -jar siphonix.jar \
	--plugin-dir /opt/siphonix/plugins \
	--plugin-discovery-mode watch-auto
```

### Command Mode

```bash
java -jar siphonix.jar --plugin-dir /opt/siphonix/plugins plugin list
java -jar siphonix.jar --plugin-dir /opt/siphonix/plugins plugin watch status
```

## Environment Variables

1. `TARGET_URL`: target service endpoint exposed via plugin context.
2. `SIPHONIX_CONFIG`: optional initial scenario source path.
3. `SIPHONIX_PLUGIN_DIR`: plugin directory fallback.
4. `SIPHONIX_PLUGIN_DISCOVERY_MODE`: fallback mode (`startup-only|watch-auto|watch-manual`).
5. `SIPHONIX_PLUGIN_ARTIFACT`: legacy single plugin artifact path.

## Samples

Sample stacks for YAML, JSON, and XML are available in `samples/` and now use plugin directory mode (`./plugins` mounted to `/opt/siphonix/plugins`) with default discovery mode `watch-auto`.

See the per-sample READMEs for run commands and plugin exploration commands.

## Additional Technical Documentation

Detailed architecture and operational notes for the current implementation are available in `documentation/ACTUAL_STATE.md`.

