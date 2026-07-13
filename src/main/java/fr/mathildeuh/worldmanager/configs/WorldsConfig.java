package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public class WorldsConfig {


    public static void loadWorlds() {
        if (!WorldManager.configFile.exists()) {
            WorldManager.worldsConfig.set("worlds", null);
            try {
                WorldManager.worldsConfig.save(WorldManager.configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        ConfigurationSection worlds = WorldManager.worldsConfig.getConfigurationSection("worlds");

        if (worlds == null) {
            return;
        }

        for (String worldName : worlds.getKeys(false)) {
            String type = WorldManager.worldsConfig.getString("worlds." + worldName + ".type");
            String generator = WorldManager.worldsConfig.getString("worlds." + worldName + ".generator");
            String environment = WorldManager.worldsConfig.getString("worlds." + worldName + ".environment");
            createWorld(worldName, type, environment, generator);
        }

        // Charger les game rules après que les mondes soient créés
        Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(WorldManager.class), WorldsConfig::loadGameRules, 20L);
    }

    private static void createWorld(String name, String type, String environment, String generator) {
        WorldCreator worldCreator = new WorldCreator(name);

        if (environment != null) {
            try {
                World.Environment env = World.Environment.valueOf(environment.toUpperCase(Locale.ROOT));
                worldCreator.environment(env);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid environment: " + environment);
                return;
            }
        }

        if (type != null) {
            try {
                WorldType worldType = WorldType.valueOf(type.toUpperCase(Locale.ROOT));
                worldCreator.type(worldType);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid world type: " + type);
                return;
            }
        }

        if (generator != null) {
            worldCreator.generator(generator);
        }
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WorldManager.class), worldCreator::createWorld);
    }

    // Méthode pour ajouter un monde au fichier de configuration
    public static void addWorld(CommandSender player, String name, String type, World.Environment environement, String generator) {
        WorldManager.worldsConfig.set("worlds." + name + ".type", type);
        WorldManager.worldsConfig.set("worlds." + name + ".environment", environement.name());
        WorldManager.worldsConfig.set("worlds." + name + ".generator", generator);
        WorldManager.worldsConfig.set("worlds." + name + ".createdBy", player.getName());
        try {
            WorldManager.worldsConfig.save(WorldManager.configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeWorld(String name) {
        WorldManager.worldsConfig.set("worlds." + name, null);
        try {
            WorldManager.worldsConfig.save(WorldManager.configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCreator(String name) {
        String creator = WorldManager.worldsConfig.getString("worlds." + name + ".createdBy");
        return creator == null ? "Unknown" : creator;
    }

    @SuppressWarnings("unchecked")
    public static void loadGameRules() {
        ConfigurationSection worlds = WorldManager.worldsConfig.getConfigurationSection("worlds");

        if (worlds == null) {
            Bukkit.getLogger().info("No worlds configuration found, skipping game rules loading");
            return;
        }

        int loadedRules = 0;
        int skippedRules = 0;
        var logger = Bukkit.getLogger();

        for (String worldName : worlds.getKeys(false)) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                logger.fine("World '" + worldName + "' not loaded, skipping game rules");
                continue;
            }

            ConfigurationSection gameRulesSection = WorldManager.worldsConfig.getConfigurationSection("worlds." + worldName + ".gameRules");
            if (gameRulesSection == null) {
                logger.fine("No game rules saved for world '" + worldName + "'");
                continue;
            }

            Set<String> ruleNames = gameRulesSection.getKeys(false);
            if (ruleNames.isEmpty()) {
                continue;
            }

            for (String gameRuleName : ruleNames) {
                try {
                    GameRule<?> gameRule = GameRule.getByName(gameRuleName);

                    if (gameRule == null) {
                        logger.warning("GameRule '" + gameRuleName + "' not recognized (may be from a mod or newer MC version)");
                        skippedRules++;
                        continue;
                    }

                    Object value = gameRulesSection.get(gameRuleName);
                    if (value == null) {
                        logger.warning("GameRule '" + gameRuleName + "' has null value for world '" + worldName + "'");
                        skippedRules++;
                        continue;
                    }

                    Class<?> ruleType = gameRule.getType();

                    if (ruleType == Boolean.class && value instanceof Boolean boolValue) {
                        world.setGameRule((GameRule<Boolean>) gameRule, boolValue);
                        loadedRules++;
                        logger.fine("✓ GameRule '" + gameRuleName + "' → " + value + " (World: " + worldName + ")");
                    } else if (ruleType == Integer.class && value instanceof Number numValue) {
                        world.setGameRule((GameRule<Integer>) gameRule, numValue.intValue());
                        loadedRules++;
                        logger.fine("✓ GameRule '" + gameRuleName + "' → " + value + " (World: " + worldName + ")");
                    } else {
                        logger.warning("GameRule '" + gameRuleName + "' has unexpected type: " + value.getClass().getSimpleName());
                        skippedRules++;
                    }
                } catch (Exception e) {
                    logger.severe("Failed to load GameRule '" + gameRuleName + "' for world '" + worldName + "': " + e.getMessage());
                    skippedRules++;
                }
            }
        }

        logger.info("GameRules loading complete - Loaded: " + loadedRules + " | Skipped: " + skippedRules);
    }
}
