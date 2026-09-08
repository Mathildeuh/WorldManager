package fr.mathildeuh.worldmanager.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {
    public static String RESOURCE_URL = "";
    private static final int TIMEOUT_MILLIS = 5000;
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
        SchedulerUtil.runAsync(() -> {
            try {
                long timestamp = System.currentTimeMillis();
                String urlWithTimestamp = "https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId + "/~&timestamp=" + timestamp;
                URLConnection connection = URI.create(urlWithTimestamp).toURL().openConnection();
                connection.setConnectTimeout(TIMEOUT_MILLIS);
                connection.setReadTimeout(TIMEOUT_MILLIS);
                try (InputStream is = connection.getInputStream(); Scanner scann = new Scanner(is)) {
                    if (scann.hasNext()) {
                        String version = scann.next();
                        SchedulerUtil.runGlobal(() -> consumer.accept(version));
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().info("Unable to check for updates: " + e.getMessage());
            }
        });
    }

}
