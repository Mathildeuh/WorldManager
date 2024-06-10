package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.messages.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class LangConfig {

    File file;
    FileConfiguration config;

    public LangConfig(File file) {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public void sendFormat(CommandSender sender, String path) {
        new MessageManager(sender).parse(getString(path));
    }

    public void sendFormat(CommandSender sender, String path, Object... objects) {
        String message = getString(path);
        for (int i = 0; i < objects.length; i++) {
            message = message.replace("{" + i + "}", objects[i].toString());
        }
        new MessageManager(sender).parse(message);
    }

}
