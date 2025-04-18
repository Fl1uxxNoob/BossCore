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
import org.bukkit.event.entity.EntityTargetEvent;
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
            // Rimuovi i drop
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Ferma l'evento
            plugin.getBossEvent().stopEvent();
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();

        // Se è il boss dell'evento e non può attaccare (secondo la config)
        if (entity instanceof IronGolem && entity.hasMetadata("bossevent") &&
                !plugin.getConfig().getBoolean("event.boss.can-attack", false)) {
            // Cancella il targeting
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Se l'evento è in corso, mostra la scoreboard
        if (plugin.getBossEvent().isRunning()) {
            plugin.getScoreboardManager().showEventScoreboard(player);
        } else if (plugin.getBossEvent().isStarting()) {
            // Se è in corso il countdown, mostra la scoreboard di countdown
            int remainingTime = plugin.getConfig().getInt("event.countdown", 15);
            plugin.getScoreboardManager().showCountdownScoreboard(player, remainingTime);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Se un giocatore esce durante l'evento, rimuovilo dalla classifica
        if (plugin.getBossEvent().isRunning()) {
            plugin.getBossEvent().removePlayerFromRanking(player.getUniqueId());
        }
    }
}