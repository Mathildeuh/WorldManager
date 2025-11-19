package fr.mathildeuh.worldmanager.configs;

import fr.mathildeuh.worldmanager.messages.MessageUtils;
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

    // Retourne le message formaté en MiniMessage (String) avec placeholders remplacés
    public String formatMessage(String path, Object... objects) {
        String message = getString(path);
        if (message == null) return null;
        for (int i = 0; i < objects.length; i++) {
            message = message.replace("{" + i + "}", objects[i].toString());
        }
        return message;
    }

    public void sendError(CommandSender target, String path, Object... objects) {
        String mini = formatMessage(path, objects);
        if (mini == null) return;
        MessageUtils.send(target, MessageUtils.wrapError(MessageUtils.parseMini(mini)));
    }

    public void sendSuccess(CommandSender target, String path, Object... objects) {
        String mini = formatMessage(path, objects);
        if (mini == null) return;
        MessageUtils.send(target, MessageUtils.wrapSuccess(MessageUtils.parseMini(mini)));
    }

    public void sendWaiting(CommandSender target, String path, Object... objects) {
        String mini = formatMessage(path, objects);
        if (mini == null) return;
        MessageUtils.send(target, MessageUtils.wrapWaiting(MessageUtils.parseMini(mini)));
    }

}
