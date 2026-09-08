package fr.mathildeuh.worldmanager.util;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guards against concurrent long-running/destructive operations (pregen, backup, restore,
 * unload, delete) racing each other on the same world. {@link #tryLock(String)} is the single
 * atomic gate every such operation must pass before doing any real work; the caller is
 * responsible for calling {@link #unlock(String)} on every exit path once it's done.
 */
public final class WorldOperationLock {

    private static final Set<String> locked = ConcurrentHashMap.newKeySet();

    private WorldOperationLock() { }

    /** Atomically claims the lock for a world. Returns false if another operation already holds it. */
    public static boolean tryLock(String worldName) {
        return locked.add(worldName.toLowerCase(Locale.ROOT));
    }

    public static void unlock(String worldName) {
        locked.remove(worldName.toLowerCase(Locale.ROOT));
    }
}
