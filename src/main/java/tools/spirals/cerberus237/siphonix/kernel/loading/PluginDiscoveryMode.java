package tools.spirals.cerberus237.siphonix.kernel.loading;

/**
 * Discovery behavior options for plugin folder runtime.
 *
 * <ul>
 * <li>{@code startup-only}: only load plugins at startup.</li>
 * <li>{@code watch-auto}: continuously watch and auto-activate changes.</li>
 * <li>{@code watch-manual}: watch and index changes, but require explicit activation.</li>
 * </ul>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
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

    /**
     * Parses textual discovery mode.
     *
     * @param rawValue raw mode value.
     * @return parsed mode, defaulting to startup-only for blank input.
     */
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
