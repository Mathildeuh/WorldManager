package fr.mathildeuh.worldmanager.events;

import fr.mathildeuh.worldmanager.WorldManager;
import fr.mathildeuh.worldmanager.messages.MessageManager;
import fr.mathildeuh.worldmanager.util.UpdateChecker;
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
import java.util.UUID;

public class JoinListener implements Listener {
    private final Set<String> updateInfoSent = new HashSet<>();
    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.isOp() || JavaPlugin.getPlugin(WorldManager.class).isUpdated()) return;

        final String name = player.getName();
        if (this.updateInfoSent.contains(name)) return;

        new MessageManager(event.getPlayer()).parse("<color:#7d66ff>{</color><color:#6258a6>------</color> <color:#02a876>World Manager</color> <color:#6258a6>------</color><color:#7d66ff>}</color>\n");
        new MessageManager(event.getPlayer()).parse("");
        new MessageManager(event.getPlayer()).parse("<dark_red> ⚠</dark_red><color:#ffa1f9> A new update is available !</color> <dark_red>⚠</dark_red>");
        new MessageManager(event.getPlayer()).parse("<click:open_url:' " + UpdateChecker.RESOURCE_URL + "'>   <color:#7471b0>➥</color> <color:#ff9900>Click here to download</color></click>");
        new MessageManager(event.getPlayer()).parse("");
        new MessageManager(event.getPlayer()).parse("<gray>Patch note: </gray> <green>"  + getLatestReleaseNote() + "</green>");
        new MessageManager(event.getPlayer()).parse("<color:#7d66ff>{</color><color:#6258a6>--------------------------</color><color:#7d66ff>}</color>");
        this.updateInfoSent.add(name);
        //
    }

    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases/latest";

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
                System.out.println("Could not fetch the latest release note. Status code: " + response.statusCode());
                return "";
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Could not fetch the latest release note.");
            return "";
        }
    }
}
