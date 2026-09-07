package fr.mathildeuh.worldmanager.commands.subcommands.pregenerator;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import fr.mathildeuh.worldmanager.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class ChunkGenerator {

    private final World world;
    private final int totalChunks; // Total number of chunks to generate
    private final Location center;
    private final BossBar bossBar;
    private final Player player;
    private boolean paused = false;
    private boolean generating = false;
    private long startTime;
    private long elapsedTime;
    private int generatedChunkCount = 0;
    private int currentX, currentZ;
    private int gridSize;

    public ChunkGenerator(World world, Player player, Integer totalChunks, Location center) {

        this.world = world;
        this.totalChunks = totalChunks;
        this.center = center;
        this.player = player;

        bossBar = Bukkit.createBossBar("Chunk Generation Progress", BarColor.BLUE, BarStyle.SOLID);

        if (player != null) {
            bossBar.addPlayer(player);
        }

    }


    public void start() {
        generating = true;
        startTime = System.currentTimeMillis();
        int centerX = center.getBlockX() >> 4;
        int centerZ = center.getBlockZ() >> 4;

        // Calculate the grid size needed to cover the totalChunks
        gridSize = (int) Math.ceil(Math.sqrt(Math.max(totalChunks, 1)));
        currentX = -gridSize / 2;
        currentZ = -gridSize / 2;

        WorldManagerCommand.activeGenerators.put(world.getName(), this);
        scheduleNextChunk(centerX, centerZ);
    }

    public void stop() {
        generating = false;
        bossBar.removeAll();
        WorldManagerCommand.activeGenerators.remove(world.getName(), this);
        WorldManager.langConfig.sendSuccess(player, "pregen.finished", generatedChunkCount);
        Bukkit.getLogger().info("[WorldManager] Chunk generation for " + world.getName() + " completed! "
                + generatedChunkCount + "/" + totalChunks + " chunks generated.");
        if (generatedChunkCount > 0) {
            Bukkit.getLogger().info("[WorldManager] Elapsed time: " + elapsedTime + "ms, average "
                    + (elapsedTime / (double) generatedChunkCount) + "ms/chunk, "
                    + (generatedChunkCount / Math.max(elapsedTime, 1) * 1000.0) + " chunks/s.");
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isGenerating() {
        return generating;
    }

    private void scheduleNextChunk(int centerX, int centerZ) {
        if (!generating) {
            return;
        }
        if (paused) {
            SchedulerUtil.runGlobalDelayed(() -> scheduleNextChunk(centerX, centerZ), 5L);
            return;
        }
        if (generatedChunkCount >= totalChunks) {
            stop();
            return;
        }

        int chunkX = centerX + currentX;
        int chunkZ = centerZ + currentZ;
        SchedulerUtil.runAtRegion(world, chunkX, chunkZ, () -> {
            if (!generating) {
                return;
            }
            if (paused) {
                SchedulerUtil.runGlobalDelayed(() -> scheduleNextChunk(centerX, centerZ), 5L);
                return;
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            chunk.load(true);
            generatedChunkCount++;
            updateBossBar();
            advancePointers();

            if (generatedChunkCount >= totalChunks) {
                stop();
                return;
            }
            SchedulerUtil.runGlobalDelayed(() -> scheduleNextChunk(centerX, centerZ), 1L);
        });
    }

    private void advancePointers() {
        currentZ++;
        if (currentZ > gridSize / 2) {
            currentZ = -gridSize / 2;
            currentX++;
            if (currentX > gridSize / 2) {
                currentX = -gridSize / 2;
            }
        }
    }

    private void updateBossBar() {
        elapsedTime = System.currentTimeMillis() - startTime;
        double progress = Math.min(1.0, (double) generatedChunkCount / totalChunks);
        bossBar.setProgress(progress);

        long remainingTime = (long) ((elapsedTime / (double) generatedChunkCount) * (totalChunks - generatedChunkCount));

        long seconds = remainingTime / 1000;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        String remainingTimeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        bossBar.setTitle(String.format("Chunk Generation: %d/%d - ETA: %s",
                generatedChunkCount, totalChunks, remainingTimeFormatted));
    }
}
