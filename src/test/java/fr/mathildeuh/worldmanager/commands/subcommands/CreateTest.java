package fr.mathildeuh.worldmanager.commands.subcommands;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.configs.LangConfig;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateTest {
    @TempDir Path directory;
    private LangConfig previousLanguage;
    private final List<Object> messages = new ArrayList<>();
    private final CommandSender sender = (CommandSender) Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class}, (proxy, method, args) -> {
                if (method.getName().equals("sendMessage")) {
                    messages.add(args[0]);
                    return null;
                }
                throw new AssertionError("Unexpected sender access: " + method.getName());
            });

    @BeforeEach
    void loadMessages() throws Exception {
        previousLanguage = WorldManager.langConfig;
        Path language = directory.resolve("en.yml");
        try (var resource = getClass().getResourceAsStream("/lang/en.yml")) {
            assertNotNull(resource);
            Files.copy(resource, language);
        }
        WorldManager.langConfig = new LangConfig(language.toFile());
    }

    @AfterEach
    void restoreMessages() {
        WorldManager.langConfig = previousLanguage;
    }

    private class CapturingCreate extends Create {
        World.Environment environment;
        WorldType type;
        Long seed;
        String generator;

        CapturingCreate() { super(sender); }

        @Override
        public void createWorld(CommandSender sender, String name, World.Environment environment,
                                WorldType type, Long seed, String generator) {
            this.environment = environment;
            this.type = type;
            this.seed = seed;
            this.generator = generator;
        }
    }

    @ParameterizedTest
    @CsvSource({"normal,NORMAL", "the_end,THE_END", "end,THE_END", "nether,NETHER", "the_nether,NETHER", "THE_END,THE_END"})
    void emptyWithoutSeedPreservesRequestedDimension(String type, World.Environment environment) {
        CapturingCreate create = new CapturingCreate();
        create.run("test", type, "EmPtY", null);
        assertEquals(environment, create.environment);
        assertEquals("Empty", create.generator);
        assertNotNull(create.seed);
    }

    @ParameterizedTest
    @CsvSource({"normal,NORMAL", "the_end,THE_END", "nether,NETHER"})
    void emptyWithSeedPreservesRequestedDimensionAndSeed(String type, World.Environment environment) {
        CapturingCreate create = new CapturingCreate();
        create.run("test", type, "-12345", "empty");
        assertEquals(environment, create.environment);
        assertEquals(-12345L, create.seed);
        assertEquals("Empty", create.generator);
    }

    @Test
    void guiGeneratorWithNoSeedPreservesEndDimension() {
        CapturingCreate create = new CapturingCreate();
        create.run("test", "the_end", null, "Empty");
        assertEquals(World.Environment.THE_END, create.environment);
        assertEquals("Empty", create.generator);
    }

    @Test
    void legacyEmptyTypeRemainsAnEmptyOverworld() {
        CapturingCreate create = new CapturingCreate();
        create.run("test", "empty", "12345", null);
        assertEquals(World.Environment.NORMAL, create.environment);
        assertEquals(WorldType.FLAT, create.type);
        assertEquals(12345L, create.seed);
        assertEquals("Empty", create.generator);
    }

    @Test
    void customGeneratorAndTypeRemainUnchanged() {
        CapturingCreate create = new CapturingCreate();
        create.run("test", "amplified", "12345", "CustomPlugin:terrain");
        assertEquals(World.Environment.NORMAL, create.environment);
        assertEquals(WorldType.AMPLIFIED, create.type);
        assertEquals(12345L, create.seed);
        assertEquals("CustomPlugin:terrain", create.generator);
    }

    @ParameterizedTest
    @CsvSource({"invalid", "9223372036854775808"})
    void invalidSeedDoesNotStartCreation(String seed) {
        CapturingCreate create = new CapturingCreate();
        create.run("test", "the_end", seed, "Empty");
        assertNull(create.environment);
        assertEquals(1, messages.size());
    }
}
