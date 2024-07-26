package fr.mathildeuh.worldmanager.worlds;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class EmptyWorldGenerator extends ChunkGenerator {
    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        // Retourner un chunk vide (tout en air)
        return createChunkData(world);
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        // Retourner une position de spawn fixe (par exemple, au centre du monde) pour les joueurs
        return new Location(world, 0, 64, 0);
    }
}
