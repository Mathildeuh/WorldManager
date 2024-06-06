package fr.mathildeuh.worldmanager.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {
    public static String RESOURCE_URL = "";
    private final JavaPlugin plugin;
    private final int resourceId;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        RESOURCE_URL = "https://www.spigotmc.org/resources/worldmanager." + resourceId + "/";
    }

    public static String getURL() {
        return RESOURCE_URL;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
                long timestamp = System.currentTimeMillis(); // Obtenez le timestamp actuel
                String urlWithTimestamp = "https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId + "/~&timestamp=" + timestamp;
                InputStream is = new URL(urlWithTimestamp).openStream();
                try (Scanner scann = new Scanner(is)) {
                    if (scann.hasNext()) {
                        consumer.accept(scann.next());
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().info("Unable to check for updates: " + e.getMessage());
            }
        });
    }

}

