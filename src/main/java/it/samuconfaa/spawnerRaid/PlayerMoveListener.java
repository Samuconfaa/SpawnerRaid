package it.samuconfaa.spawnerRaid;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import it.samuconfaa.spawnerRaid.SpawnerRaid;

public class PlayerMoveListener implements Listener {

    private final SpawnerRaid plugin;

    public PlayerMoveListener(SpawnerRaid plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Controlla solo se il giocatore si è effettivamente mosso (non solo rotazione)
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            // Attiva i mob vicini al giocatore
            plugin.getSpawnerManager().activateMobsNearPlayer(player);
        }
    }
}