package net.fliuxx.bossCore.listeners;

import net.fliuxx.bossCore.BossCore;
import net.fliuxx.bossCore.managers.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public class BossListener implements Listener {

    private final BossCore plugin;

    public BossListener(BossCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        if (damaged instanceof IronGolem && damaged.hasMetadata("bossevent")) {
            if (damaged.hasMetadata("countdown")) {
                event.setCancelled(true);
                return;
            }

            if (plugin.getBossEvent().isRunning() && damager instanceof Player) {
                Player player = (Player) damager;
                plugin.getBossEvent().registerHit(player);
                event.setDamage(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof IronGolem && entity.hasMetadata("bossevent")) {
            event.setCancelled(true);
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
                (!plugin.getConfig().getBoolean("event.boss.can-attack", false) || entity.hasMetadata("countdown"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getBossEvent().isStarting() || plugin.getBossEvent().getBoss() == null) {
            return;
        }

        IronGolem boss = plugin.getBossEvent().getBoss();
        if (boss.hasMetadata("no_collision")) {
            if (player.getLocation().distance(boss.getLocation()) < 1.5) {
                Location playerLoc = player.getLocation();
                Vector direction = playerLoc.toVector().subtract(boss.getLocation().toVector()).normalize();

                if (direction.length() > 0) {
                    player.setVelocity(direction.multiply(0.5));
                }
            }
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