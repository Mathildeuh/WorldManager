package fr.mathildeuh.worldmanager.commands.subcommands.pregenerator;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.commands.WorldManagerCommand;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class ChunkGenerator {

    private final World world;
    private final int totalChunks; // Total number of chunks to generate
    private final Location center;
    private final BossBar bossBar;
    private final Set<Chunk> generatedChunks = new HashSet<>();
    private final Player player;
    private final int chunksPerTick = 10; // Number of chunks to generate per tick
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
        gridSize = (int) Math.ceil(Math.sqrt(totalChunks));
        currentX = -gridSize / 2;
        currentZ = -gridSize / 2;

        new ChunkGenerationTask(centerX, centerZ, gridSize).runTaskTimer(JavaPlugin.getPlugin(WorldManager.class), 0, 5);
    }

    public void stop() {
        generating = false;
        bossBar.removeAll();
        WorldManagerCommand.generator = null;
        WorldManager.langConfig.sendSuccess(player, "pregen.finished", generatedChunkCount);
        System.out.println("Chunk generation completed! " + generatedChunkCount + "/" + totalChunks + " chunks generated.");
        System.out.println("Elapsed time: " + elapsedTime + "ms");
        System.out.println("Average time per chunk: " + (elapsedTime / (double) generatedChunkCount) + "ms");
        System.out.println("Average chunks per second: " + (generatedChunkCount / (double) elapsedTime) + "chunks/s");
        System.out.println(Bukkit.getWorlds().get(0).getLoadedChunks().length + " chunks loaded.");
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

    private class ChunkGenerationTask extends BukkitRunnable {
        private final int centerX;
        private final int centerZ;
        private final int gridSize;

        public ChunkGenerationTask(int centerX, int centerZ, int gridSize) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.gridSize = gridSize;
        }

        @Override
        public void run() {
            if (!generating) {
                cancel();
                return;
            }

            if (paused) {
                return;
            }

            for (int i = 0; i < chunksPerTick; i++) {
                if (generatedChunkCount >= totalChunks) {
                    stop();
                    cancel();
                    return;
                }

                int chunkX = centerX + currentX;
                int chunkZ = centerZ + currentZ;

                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                if (!generatedChunks.contains(chunk)) {
                    chunk.load(true);
                    generatedChunks.add(chunk);
                    generatedChunkCount++;
                    updateBossBar();
                }

                currentZ++;
                if (currentZ > gridSize / 2) {
                    currentZ = -gridSize / 2;
                    currentX++;
                    if (currentX > gridSize / 2) {
                        currentX = -gridSize / 2;
                    }
                }
            }
        }

        private void updateBossBar() {
            elapsedTime = System.currentTimeMillis() - startTime;
            double progress = (double) generatedChunkCount / totalChunks;
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
}
