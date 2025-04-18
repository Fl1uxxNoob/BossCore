package net.fliuxx.bossCore.listeners;

import net.fliuxx.bossCore.BossCore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BossListener implements Listener {

    private final BossCore plugin;

    public BossListener(BossCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        // Controlla se l'entità danneggiata è il boss
        if (damaged instanceof IronGolem && damaged.hasMetadata("bossevent") && plugin.getBossEvent().isRunning()) {
            // Controlla se il danno proviene da un giocatore
            if (damager instanceof Player) {
                Player player = (Player) damager;

                // Registra l'hit
                plugin.getBossEvent().registerHit(player);

                // Previeni il danno reale (useremo il nostro sistema di vita)
                event.setDamage(0);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Se il boss muore (anche se non dovrebbe mai succedere grazie al nostro sistema)
        if (entity instanceof IronGolem && entity.hasMetadata("bossevent") && plugin.getBossEvent().isRunning()) {
            // Ferma l'evento
            plugin.getBossEvent().stopEvent();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Se l'evento è in corso, mostra la scoreboard
        if (plugin.getBossEvent().isRunning()) {
            plugin.getScoreboardManager().showScoreboard(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Non c'è bisogno di rimuovere la scoreboard poiché il giocatore sta uscendo
    }
}