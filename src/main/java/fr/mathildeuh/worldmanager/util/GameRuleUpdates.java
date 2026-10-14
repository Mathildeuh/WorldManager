package fr.mathildeuh.worldmanager.util;

import fr.mathildeuh.worldmanager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Read-modify-write operations for world rules belong to the global scheduler. */
public final class GameRuleUpdates {
    private GameRuleUpdates() { }

    public static <T> CompletableFuture<Boolean> update(World expectedWorld, GameRule<T> rule, UnaryOperator<T> change) {
        WorldManager plugin = WorldManager.getInstance();
        if (!plugin.acceptsTasks()) return CompletableFuture.completedFuture(false);
        return update(() -> {
            if (!plugin.acceptsTasks()) return null;
            // A GUI opened for an old instance must not change a replacement world with the same name.
            return Bukkit.getWorld(expectedWorld.getUID()) == expectedWorld ? expectedWorld : null;
        }, rule, change, SchedulerUtil::runGlobal);
    }

    static <T> CompletableFuture<Boolean> update(Supplier<World> lookup, GameRule<T> rule,
                                                UnaryOperator<T> change, Consumer<Runnable> globalScheduler) {
        CompletableFuture<Boolean> completion = new CompletableFuture<>();
        try {
            globalScheduler.accept(() -> {
                try {
                    World world = lookup.get();
                    T current = world == null ? null : world.getGameRuleValue(rule);
                    if (current == null) completion.complete(false);
                    else completion.complete(world.setGameRule(rule, change.apply(current)));
                } catch (Exception e) {
                    completion.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            completion.completeExceptionally(e);
        }
        return completion;
    }

    public static int adjustInteger(int value, int delta) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, (long) value + delta));
    }
}
