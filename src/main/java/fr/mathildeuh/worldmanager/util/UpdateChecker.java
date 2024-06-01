package fr.mathildeuh.worldmanager.util;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final int RESOURCE_ID = 117043;
    public static final String RESOURCE_URL = "https://www.spigotmc.org/resources/worldmanager." + RESOURCE_ID + "/";

    public static void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(WorldManager.class), () -> {
            try (final InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID + "/~").openStream(); Scanner scanner = new Scanner(is)) {
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (final IOException ignored) {
            }
        });
    }
}

