package net.fliuxx.bossCore.abilities;

import net.fliuxx.bossCore.BossCore;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class PushBackAbility implements BossAbility {
    private final BossCore plugin;
    private int hitCounter = 0;

    public PushBackAbility(BossCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canActivate() {
        if (!isEnabled()) {
            return false;
        }

        int requiredHits = plugin.getConfig().getInt("abilities.pushback.trigger-hits", 10);

        if (hitCounter >= requiredHits) {
            hitCounter = 0;
            return true;
        }

        return false;
    }

    public void incrementHitCounter() {
        if (isEnabled()) {
            hitCounter++;
        }
    }

    @Override
    public void activate(IronGolem boss, List<Player> nearbyPlayers) {
        if (boss == null || boss.isDead() || nearbyPlayers.isEmpty()) {
            return;
        }

        double range = plugin.getConfig().getDouble("abilities.pushback.range", 5.0);
        double powerXZ = plugin.getConfig().getDouble("abilities.pushback.power-horizontal", 2.0);
        double powerY = plugin.getConfig().getDouble("abilities.pushback.power-vertical", 0.5);
        String soundName = plugin.getConfig().getString("abilities.pushback.sound", "ENTITY_GENERIC_EXPLODE");
        String message = plugin.getConfig().getString("abilities.pushback.message", "&c&lIl boss respinge tutti i giocatori!");

        List<Player> affectedPlayers = nearbyPlayers.stream()
                .filter(player -> player.getLocation().distance(boss.getLocation()) <= range)
                .toList();

        if (affectedPlayers.isEmpty()) {
            return;
        }

        for (Player player : affectedPlayers) {
            Vector direction = player.getLocation().toVector().subtract(boss.getLocation().toVector());

            if (direction.length() > 0) {
                direction = direction.normalize();
                direction.setX(direction.getX() * powerXZ);
                direction.setZ(direction.getZ() * powerXZ);
                direction.setY(powerY);

                player.setVelocity(direction);
            }

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Suono non valido nella configurazione: " + soundName);
                player.playSound(player.getLocation(), Sound.EXPLODE, 1.0f, 1.0f);
            }
        }

        if (message != null && !message.isEmpty()) {
            String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
            for (Player player : nearbyPlayers) {
                player.sendMessage(coloredMessage);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("abilities.pushback.enabled", true);
    }

    @Override
    public void reset() {
        hitCounter = 0;
    }
}
