package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.configs.WorldsConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

/** Enforces the "weatherLocked" per-world flag: blocks the world from ever starting a storm. */
public class WorldFlagsListener implements Listener {

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.toWeatherState()) {
            return; // Always allow clearing up.
        }
        if (WorldsConfig.getFlag(event.getWorld().getName(), "weatherLocked", false)) {
            event.setCancelled(true);
        }
    }
}
