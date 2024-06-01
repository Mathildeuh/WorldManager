package fr.mathildeuh.worldmanager.commands.subcommands.pregen;

import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WorldPreGenerator extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final World world;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private int currentX;
    private int currentZ;
    private int totalChunks;
    private int processedChunks = 0;
    private BossBar bossBar;
    private MessageManager message;
    Player player;

    public WorldPreGenerator(JavaPlugin plugin, Player player, World world, int centerX, int centerZ, int radius) {
        this.plugin = plugin;
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.currentX = centerX - radius;
        this.currentZ = centerZ - radius;
        this.totalChunks = (radius * 2 + 1) * (radius * 2 + 1);
        this.message = new MessageManager(player);
        this.player = player;
        createBossBar();
    }

    @Override
    public void run() {
        if (processedChunks >= totalChunks) {
            completeGeneration();
            return;
        }

        if (currentZ > centerZ + radius) {
            currentX++;
            currentZ = centerZ - radius;
        }

        if (currentX > centerX + radius) {
            completeGeneration();
            return;
        }

        Chunk chunk = world.getChunkAt(currentX, currentZ);
        chunk.load(true);
        processedChunks++;
        currentZ++;

        if (processedChunks % 50 == 0 || processedChunks == totalChunks) {

                message.parse(MessageManager.MessageType.WAITING, processedChunks + " chunks have been pre-generated for world: " + world.getName());
        }

        if (processedChunks == totalChunks) {
            plugin.getLogger().info("Pre-generated " + processedChunks + " chunks for world: " + world.getName());
        }

        updateBossBar();
    }

    public void start() {
        this.runTaskTimer(plugin, 0, 1); // Runs every tick
    }

    private void createBossBar() {
        bossBar = Bukkit.createBossBar("Pre-generation progress", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
    }

    private void updateBossBar() {
        double progress = (double) processedChunks / totalChunks;
        bossBar.setProgress(progress);
        bossBar.setTitle("Pre-generated " + processedChunks + " / " + totalChunks + " chunks for world: " + world.getName());
    }

    private void removeBossBar() {
        bossBar.removePlayer(player);
        bossBar.removeAll();
    }

    private void completeGeneration() {
        cancel();
        bossBar.setProgress(1.0);
        bossBar.setTitle("World pre-generation completed for world: " + world.getName());
        plugin.getLogger().info("World pre-generation completed for world: " + world.getName());
        message.parse(MessageManager.MessageType.SUCCESS, "World pre-generation completed for world: " + world.getName());
        removeBossBar();
    }
}
