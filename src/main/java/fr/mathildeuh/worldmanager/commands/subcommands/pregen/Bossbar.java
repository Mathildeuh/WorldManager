package fr.mathildeuh.worldmanager.commands.subcommands.pregen;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import static fr.mathildeuh.worldmanager.commands.subcommands.pregen.WorldPreGenerator.processedChunks;
import static fr.mathildeuh.worldmanager.commands.subcommands.pregen.WorldPreGenerator.totalChunks;

public class Bossbar {

    public static BossBar bossBar;

    public static void createBossBar(Player player) {
        bossBar = Bukkit.createBossBar("Pre-generation progress", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
    }

    public static void updateBossBar(World world) {
        double progress = (double) processedChunks / totalChunks;
        bossBar.setProgress(progress);
        bossBar.setTitle("Pre-generated " + processedChunks + " / " + totalChunks + " chunks for world: " + world.getName());
    }

    public static void removeBossBar(Player player) {
        bossBar.removePlayer(player);
        bossBar.removeAll();
    }

}
