package fr.mathildeuh.worldmanager.util;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import fr.mathildeuh.worldmanager.worlds.EmptyWorldGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class WorldLifecycleTest {
    public enum Result {
        SUCCESS, FAIL_PLAYERS_JOINING, FAIL_PLAYERS_PRESENT, FAIL_ALREADY_UNLOADING,
        FAIL_IS_OVERWORLD, FAIL_UNLOAD_EVENT, FAIL_IS_SHUTDOWN, FAIL_UNKNOWN
    }

    public interface AsyncServer extends Server {
        void unloadWorldAsync(World world, boolean save, Consumer<Result> callback);
    }

    private final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
            (proxy, method, args) -> { throw new AssertionError("Unexpected world access: " + method.getName()); });

    @ParameterizedTest
    @EnumSource(Result.class)
    void asynchronousUnloadWaitsForCallbackAndPropagatesRejections(Result result) {
        AtomicReference<Consumer<Result>> callback = new AtomicReference<>();
        Server server = (Server) Proxy.newProxyInstance(AsyncServer.class.getClassLoader(), new Class<?>[]{AsyncServer.class},
                (proxy, method, args) -> {
                    assertEquals("unloadWorldAsync", method.getName());
                    assertSame(world, args[0]);
                    assertEquals(false, args[1]);
                    @SuppressWarnings("unchecked") Consumer<Result> consumer = (Consumer<Result>) args[2];
                    callback.set(consumer);
                    return null;
                });
        var completion = WorldLifecycle.unload(server, world, false);
        assertFalse(completion.isDone(), "Deleting files must remain blocked until the server confirms unloading");
        callback.get().accept(result);
        assertEquals(result == Result.SUCCESS, completion.join());
    }

    @Test
    void asynchronousFailureNeverFallsBackToAnotherUnloadAttempt() {
        IllegalStateException failure = new IllegalStateException("Unload already in progress");
        Server server = (Server) Proxy.newProxyInstance(AsyncServer.class.getClassLoader(), new Class<?>[]{AsyncServer.class},
                (proxy, method, args) -> {
                    assertEquals("unloadWorldAsync", method.getName());
                    throw failure;
                });
        var completion = WorldLifecycle.unload(server, world, false);
        assertSame(failure, assertThrows(CompletionException.class, completion::join).getCause());
    }

    @Test
    void legacyServerPreservesSaveFlagAndRejectedResult() {
        AtomicBoolean called = new AtomicBoolean();
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    assertEquals("unloadWorld", method.getName());
                    assertSame(world, args[0]);
                    assertEquals(true, args[1]);
                    called.set(true);
                    return false;
                });
        assertFalse(WorldLifecycle.unload(server, world, true).join());
        assertTrue(called.get());
    }

    @Test
    void unsupportedLegacyApiCompletesExceptionally() {
        UnsupportedOperationException failure = new UnsupportedOperationException("Unavailable on this server");
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, args) -> { throw failure; });
        var completion = WorldLifecycle.unload(server, world, true);
        assertSame(failure, assertThrows(CompletionException.class, completion::join).getCause());
    }

    @ParameterizedTest
    @EnumSource(value = World.Environment.class, names = {"NORMAL", "NETHER", "THE_END"})
    void emptyMarkerLoadsBuiltInGeneratorAndPreservesDimension(World.Environment environment) {
        WorldCreator creator = new WorldCreator(org.bukkit.NamespacedKey.minecraft("empty-test")).environment(environment);
        WorldLifecycle.applyGenerator(creator, "eMpTy");
        assertInstanceOf(EmptyWorldGenerator.class, creator.generator());
        assertEquals(environment, creator.environment());
    }
}
