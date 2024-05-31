package fr.mathildeuh.worldmanager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    private boolean paused = false;
    private BossBar bossBar;

    public WorldPreGenerator(JavaPlugin plugin, World world, int centerX, int centerZ, int radius) {
        this.plugin = plugin;
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.currentX = centerX - radius;
        this.currentZ = centerZ - radius;
        this.totalChunks = (radius * 2 + 1) * (radius * 2 + 1);
        createBossBar();
    }

    @Override
    public void run() {
        if (paused) return;

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

        if (processedChunks % 100 == 0) {
            plugin.getLogger().info("Pre-generated " + processedChunks + " / " + totalChunks + " chunks for world: " + world.getName());
        }

        updateBossBar();
    }

    public void start() {
        this.runTaskTimer(plugin, 0, 1); // Runs every tick
    }

    public void pause() {
        paused = true;
        plugin.getLogger().info("World pre-generation paused for world: " + world.getName());
    }

    public void resume() {
        paused = false;
        plugin.getLogger().info("World pre-generation resumed for world: " + world.getName());
    }

    public void cancelGeneration() {
        cancel();
        removeBossBar();
        plugin.getLogger().info("World pre-generation canceled for world: " + world.getName());
    }

    private void createBossBar() {
        bossBar = Bukkit.createBossBar("Pre-generation progress", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    private void updateBossBar() {
        double progress = (double) processedChunks / totalChunks;
        bossBar.setProgress(progress);
        bossBar.setTitle("Pre-generated " + processedChunks + " / " + totalChunks + " chunks for world: " + world.getName());
    }

    private void removeBossBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.removePlayer(player);
        }
        bossBar.removeAll();
    }

    private void completeGeneration() {
        cancel();
        bossBar.setProgress(1.0);
        bossBar.setTitle("World pre-generation completed for world: " + world.getName());
        plugin.getLogger().info("World pre-generation completed for world: " + world.getName());
        Bukkit.broadcastMessage("World pre-generation completed for world: " + world.getName());
        removeBossBar();
    }
}
