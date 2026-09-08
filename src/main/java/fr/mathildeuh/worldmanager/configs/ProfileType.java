package fr.mathildeuh.worldmanager.configs;

import java.util.Locale;

/**
 * One shareable aspect of a player's per-group profile, in the spirit of
 * Multiverse-Inventories' per-group "share" settings. A group's {@code share} list in
 * {@code config.yml} picks which of these are captured/restored on group entry/exit; any aspect
 * left out is never touched by a world change at all (it just persists normally, exactly as it
 * would in a world that isn't part of any group).
 */
public enum ProfileType {
    INVENTORY("inventory"),
    HEALTH("health"),
    HUNGER("hunger"),
    EXPERIENCE("experience"),
    BED_SPAWN("bed-spawn"),
    LOCATION("location");

    private final String configKey;

    ProfileType(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }

    public static ProfileType fromConfigKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (ProfileType type : values()) {
            if (type.configKey.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
