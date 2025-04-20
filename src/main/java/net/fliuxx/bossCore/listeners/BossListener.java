package net.fliuxx.bossCore.listeners;

import net.fliuxx.bossCore.BossCore;
import net.fliuxx.bossCore.managers.ScoreboardManager;
import org.bukkit.Bukkit;
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

        if (damaged instanceof IronGolem && damaged.hasMetadata("bossevent") && plugin.getBossEvent().isRunning()) {
            if (damager instanceof Player) {
                Player player = (Player) damager;

                plugin.getBossEvent().registerHit(player);

                event.setDamage(0);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof IronGolem && entity.hasMetadata("bossevent") && plugin.getBossEvent().isRunning()) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            plugin.getBossEvent().stopEvent();
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof IronGolem && entity.hasMetadata("bossevent") &&
                !plugin.getConfig().getBoolean("event.boss.can-attack", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getBossEvent().isRunning()) {
                plugin.getScoreboardManager().showEventScoreboard(player);
            } else if (plugin.getBossEvent().isStarting()) {
                plugin.getScoreboardManager().showCountdownScoreboard(player,
                        plugin.getConfig().getInt("event.countdown", 15));
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getBossEvent().isRunning()) {
            plugin.getBossEvent().removePlayerFromRanking(player.getUniqueId());
        }

        ScoreboardManager.removeScore(player);
    }
}