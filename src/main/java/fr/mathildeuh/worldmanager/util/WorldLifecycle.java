package fr.mathildeuh.worldmanager.util;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.worlds.EmptyWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Version-independent world lifecycle boundary. No fork API is required on the classpath. */
public final class WorldLifecycle {
    private WorldLifecycle() { }

    /** "Empty" is a persisted built-in marker, not the name of a generator plugin. */
    public static void applyGenerator(WorldCreator creator, String generator) {
        if ("Empty".equalsIgnoreCase(generator)) {
            creator.generator(new EmptyWorldGenerator());
        } else if (generator != null && !generator.isEmpty()) {
            creator.generator(generator);
        }
    }

    /** Each player is inspected and teleported on their own region, including players outside this world. */
    public static CompletableFuture<Boolean> evacuate(World world, Consumer<Player> beforeTeleport) {
        Location destination = Bukkit.getWorlds().get(0).getSpawnLocation();
        List<CompletableFuture<Boolean>> teleports = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            teleports.add(result);
            SchedulerUtil.runEntity(player, () -> {
                try {
                    if (!player.getWorld().equals(world)) {
                        result.complete(true);
                        return;
                    }
                    beforeTeleport.accept(player);
                    player.teleportAsync(destination.clone()).whenComplete((success, error) -> {
                        if (error != null) result.completeExceptionally(error);
                        else result.complete(Boolean.TRUE.equals(success));
                    });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            }, () -> result.complete(true)); // A retired player no longer needs evacuation.
        }
        return CompletableFuture.allOf(teleports.toArray(new CompletableFuture<?>[0]))
                .thenApply(ignored -> teleports.stream().allMatch(future -> future.getNow(false)));
    }

    /** Called on the global scheduler. Complete only once the server has actually unloaded the world. */
    public static CompletableFuture<Boolean> unload(World world, boolean save) {
        return unload(Bukkit.getServer(), world, save);
    }

    // Separate from scheduling so both API shapes can be verified without starting a server.
    static CompletableFuture<Boolean> unload(Server server, World world, boolean save) {
        CompletableFuture<Boolean> completion = new CompletableFuture<>();
        try {
            Method asyncUnload;
            try {
                asyncUnload = server.getClass().getMethod("unloadWorldAsync", World.class, boolean.class, Consumer.class);
            } catch (NoSuchMethodException absent) {
                completion.complete(server.unloadWorld(world, save));
                return completion;
            }
            Consumer<Object> callback = result -> {
                if (result instanceof Enum<?> status) {
                    completion.complete("SUCCESS".equals(status.name()));
                } else {
                    completion.completeExceptionally(new IllegalStateException("Unrecognized world unload result: " + result));
                }
            };
            asyncUnload.invoke(server, world, save, callback);
        } catch (InvocationTargetException e) {
            completion.completeExceptionally(e.getCause());
        } catch (Exception e) {
            completion.completeExceptionally(e);
        }
        return completion;
    }

    /** Completion callbacks may arrive on another thread or after plugin shutdown. */
    public static void completeOnGlobal(String worldName, Runnable completion) {
        if (!WorldManager.getInstance().acceptsTasks()) {
            WorldOperationLock.unlock(worldName);
            return;
        }
        try {
            SchedulerUtil.runGlobal(() -> {
                if (WorldManager.getInstance().acceptsTasks()) completion.run();
                else WorldOperationLock.unlock(worldName);
            });
        } catch (org.bukkit.plugin.IllegalPluginAccessException e) {
            WorldOperationLock.unlock(worldName);
        }
    }
}
