# SiphoniX Actual State Documentation

## Overview

This document describes the implemented state of the plugin runtime and operational model.

SiphoniX currently provides:
1. Folder-based plugin discovery and loading.
2. Optional runtime watching for plugin artifacts.
3. Plugin command-line operations for lifecycle management.
4. Scenario source hand-off to plugin-side parsers and configuration managers.

## Runtime Components

### 1. Bootstrap Layer

Main entrypoint: src/main/java/tools/spirals/cerberus237/siphonix/SiphoniX.java

Responsibilities:
1. Resolve launch options from CLI + env vars.
2. Start daemon runtime or execute command mode.
3. Initialize and start plugins through PluginRegistry.
4. Wire shutdown hook for graceful stop and classloader cleanup.

### 2. Registry Layer

Registry: src/main/java/tools/spirals/cerberus237/siphonix/kernel/PluginRegistry.java

Responsibilities:
1. Register/get/remove/replace plugins.
2. Forward lifecycle operations.
3. Support typed listing through listByType(Class<T>).

### 3. Loading Layer

Artifact loader: src/main/java/tools/spirals/cerberus237/siphonix/kernel/loading/PluginArtifactLoader.java

Responsibilities:
1. Load plugin implementations via ServiceLoader.
2. Support typed loading and multi-type fallback.
3. Expose classloader ownership through LoadedPluginHandle.

Runtime loader: src/main/java/tools/spirals/cerberus237/siphonix/kernel/loading/PluginRuntimeLoader.java

Responsibilities:
1. Discover plugin jars from folder.
2. Apply discovery mode behavior.
3. Activate, replace, unload plugins.
4. Track pending artifacts in manual mode.
5. Apply scenario source when plugin implements ScenarioManagementPlugin.

Discovery mode enum: src/main/java/tools/spirals/cerberus237/siphonix/kernel/loading/PluginDiscoveryMode.java

## Discovery Modes

1. startup-only:
- Startup scan only.
- No watch thread.

2. watch-auto:
- Startup scan + periodic scan loop.
- Auto load/replace/unload according to artifact changes.

3. watch-manual:
- Startup scan applies.
- Runtime changes are recorded as pending.
- Activation requires explicit command.

## CLI Surface

Supported plugin commands:
1. plugin list
2. plugin load <jarPath>
3. plugin unload <pluginId>
4. plugin start <pluginId>
5. plugin stop <pluginId>
6. plugin reload <pluginId>
7. plugin watch on|off|status

## Configuration Inputs

Environment variables:
1. TARGET_URL
2. SIPHONIX_CONFIG
3. SIPHONIX_PLUGIN_DIR
4. SIPHONIX_PLUGIN_DISCOVERY_MODE
5. SIPHONIX_PLUGIN_ARTIFACT (legacy compatibility)

CLI options:
1. --plugin-dir <path>
2. --plugin-discovery-mode <startup-only|watch-auto|watch-manual>

## Scenario Source Responsibility

SiphoniX now forwards a generic path-based scenario source.

Format-specific interpretation is handled by plugin internals (yaml/json/xml managers), not by SiphoniX bootstrap.

## Sample Runtime Integration

The YAML/JSON/XML adaptiflow samples were updated to:
1. Mount ./plugins into /opt/siphonix/plugins.
2. Configure SIPHONIX_PLUGIN_DIR and SIPHONIX_PLUGIN_DISCOVERY_MODE.
3. Copy plugin jars into ./plugins during run scripts.

This enables immediate exploration of:
1. plugin list
2. plugin watch status
3. dynamic plugin updates in watch-auto mode

## Known Limitations

1. Runtime watch implementation currently uses periodic scanning instead of file-system event streams.
2. No persisted plugin inventory beyond process lifetime.
3. Multi-plugin-type strategy is prepared by generic loader support, but domain-specific plugin families beyond ScenarioManagementPlugin still need concrete interfaces and implementations.

## Validation Status

The current implementation state has been validated with:
1. Root build/package.
2. Root tests.
3. Adaptiflow plugin module tests.

## Author

Arléon Zemtsop (Cerberus)
