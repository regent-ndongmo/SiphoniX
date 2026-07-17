package tools.spirals.cerberus237.siphonix.kernel.loading;

public enum PluginDiscoveryMode {
    STARTUP_ONLY("startup-only"),
    WATCH_AUTO("watch-auto"),
    WATCH_MANUAL("watch-manual");

    private final String value;

    PluginDiscoveryMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PluginDiscoveryMode fromValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return STARTUP_ONLY;
        }
        String normalized = rawValue.trim().toLowerCase();
        for (PluginDiscoveryMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported plugin discovery mode: " + rawValue
                + ". Supported values: startup-only, watch-auto, watch-manual");
    }
}
