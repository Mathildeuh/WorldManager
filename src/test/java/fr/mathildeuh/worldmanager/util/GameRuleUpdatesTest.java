package fr.mathildeuh.worldmanager.util;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class GameRuleUpdatesTest {
    private final Queue<Runnable> globalTasks = new ArrayDeque<>();
    private boolean onGlobalThread;
    private Object value = true;
    private int writes;
    private boolean acceptsValue = true;
    private RuntimeException failure;

    // The rule token is irrelevant to this world stub: these tests cover scheduling and atomic updates.
    private final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
            (proxy, method, args) -> {
                assertTrue(onGlobalThread, "Game rules must never be read or changed from the GUI's entity callback");
                return switch (method.getName()) {
                    case "getGameRuleValue" -> value;
                    case "setGameRule" -> {
                        if (failure != null) throw failure;
                        writes++;
                        if (acceptsValue) value = args[1];
                        yield acceptsValue;
                    }
                    default -> throw new AssertionError("Unexpected world access: " + method.getName());
                };
            });

    private void runGlobalTask() {
        onGlobalThread = true;
        try {
            globalTasks.remove().run();
        } finally {
            onGlobalThread = false;
        }
    }

    @Test
    void booleanToggleWaitsForGlobalScheduler() {
        var result = GameRuleUpdates.<Boolean>update(() -> world, null, current -> !current, globalTasks::add);
        assertFalse(result.isDone());
        assertEquals(0, writes);
        runGlobalTask();
        assertTrue(result.join());
        assertEquals(false, value);
    }

    @Test
    void overlappingTogglesReadTheLatestValueInsteadOfLosingAnUpdate() {
        var first = GameRuleUpdates.<Boolean>update(() -> world, null, current -> !current, globalTasks::add);
        var second = GameRuleUpdates.<Boolean>update(() -> world, null, current -> !current, globalTasks::add);
        runGlobalTask();
        runGlobalTask();
        assertTrue(first.join());
        assertTrue(second.join());
        assertEquals(true, value);
        assertEquals(2, writes);
    }

    @Test
    void integerStepAndExactValueUseGlobalScheduler() {
        value = 3;
        var step = GameRuleUpdates.<Integer>update(() -> world, null,
                current -> GameRuleUpdates.adjustInteger(current, -10), globalTasks::add);
        runGlobalTask();
        assertTrue(step.join());
        assertEquals(0, value);
        var exact = GameRuleUpdates.<Integer>update(() -> world, null, current -> 12345, globalTasks::add);
        runGlobalTask();
        assertTrue(exact.join());
        assertEquals(12345, value);
    }

    @Test
    void integerIncrementsCannotOverflowIntoNegativeValues() {
        assertEquals(Integer.MAX_VALUE, GameRuleUpdates.adjustInteger(Integer.MAX_VALUE - 1, 10));
        assertEquals(0, GameRuleUpdates.adjustInteger(3, -10));
    }

    @Test
    void worldUnloadedBeforeExecutionIsNotModified() {
        var result = GameRuleUpdates.<Boolean>update(() -> null, null, current -> !current, globalTasks::add);
        runGlobalTask();
        assertFalse(result.join());
        assertEquals(0, writes);
    }

    @Test
    void unavailableRuleIsNotModified() {
        value = null;
        var result = GameRuleUpdates.<Boolean>update(() -> world, null, current -> !current, globalTasks::add);
        runGlobalTask();
        assertFalse(result.join());
        assertEquals(0, writes);
    }

    @Test
    void serverRejectionIsReportedWithoutClaimingSuccess() {
        acceptsValue = false;
        var result = GameRuleUpdates.<Boolean>update(() -> world, null, current -> !current, globalTasks::add);
        runGlobalTask();
        assertFalse(result.join());
        assertEquals(true, value);
    }

    @Test
    void updateFailureCompletesTheResultWithItsOriginalCause() {
        failure = new IllegalStateException("Setting rejected");
        var result = GameRuleUpdates.<Boolean>update(() -> world, null, current -> !current, globalTasks::add);
        runGlobalTask();
        assertSame(failure, assertThrows(CompletionException.class, result::join).getCause());
    }

    @Test
    void schedulingRejectionDoesNotLeaveAPendingResult() {
        var rejected = new RejectedExecutionException("Plugin stopping");
        var result = GameRuleUpdates.<Boolean>update(() -> world, null, current -> !current,
                task -> { throw rejected; });
        assertSame(rejected, assertThrows(CompletionException.class, result::join).getCause());
        assertEquals(0, writes);
    }
}
