package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageUtils;
import fr.mathildeuh.worldmanager.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class JoinListener implements Listener {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    private final Set<String> updateInfoSent = new HashSet<>();

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.isOp() || JavaPlugin.getPlugin(WorldManager.class).isUpdated()) return;

        final String name = player.getName();
        if (this.updateInfoSent.contains(name)) return;

        MessageUtils.sendMini(player, "<color:#7d66ff>{</color><color:#6258a6>------</color> <color:#02a876>World Manager</color> <color:#6258a6>------</color><color:#7d66ff>}</color>\n");
        MessageUtils.sendMini(player, "");
        MessageUtils.sendMini(player, "<dark_red> ⚠</dark_red><color:#ffa1f9> A new update is available !</color> <dark_red>⚠</dark_red>");
        MessageUtils.sendMini(player, "<click:open_url:' " + UpdateChecker.getURL() + "'>   <color:#7471b0>➥</color> <color:#ff9900>Click here to download</color></click>");
        MessageUtils.sendMini(player, "");
        MessageUtils.sendMini(player, "<gray>Patch note: </gray> <green>" + getLatestReleaseNote() + "</green>");
        MessageUtils.sendMini(player, "<color:#7d66ff>{</color><color:#6258a6>--------------------------</color><color:#7d66ff>}</color>");
        this.updateInfoSent.add(name);
    }

    public String getLatestReleaseNote() {
        String owner = "Mathildeuh";
        String repo = "WorldManager";
        try {
            String apiUrl = String.format(GITHUB_API_URL, owner, repo);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                String body = json.getString("body");
                int starIndex = body.indexOf('*');
                if (starIndex != -1) {
                    body = body.substring(0, starIndex);
                }
                return body;
            } else {
                Bukkit.getLogger().log(Level.INFO, "Could not fetch the latest release note. Status code: " + response.statusCode());
                return "";
            }

        } catch (IOException | InterruptedException e) {
            Bukkit.getLogger().log(Level.INFO, "Could not fetch the latest release note.");
            return "";
        }
    }
}
