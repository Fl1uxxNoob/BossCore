package net.fliuxx.bossCore.abilities;

import net.fliuxx.bossCore.BossCore;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class AbilityManager {
    private final BossCore plugin;
    private final List<BossAbility> abilities = new ArrayList<>();
    private BukkitTask checkTask;

    public AbilityManager(BossCore plugin) {
        this.plugin = plugin;

        // Registra le abilità disponibili
        registerAbilities();
    }

    private void registerAbilities() {
        // Registra l'abilità PushBack
        abilities.add(new PushBackAbility(plugin));

        // In futuro, altre abilità possono essere aggiunte qui
    }

    public void startAbilityChecks() {
        if (checkTask != null) {
            checkTask.cancel();
        }

        // Reset di tutte le abilità
        abilities.forEach(BossAbility::reset);

        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getBossEvent().isRunning() || plugin.getBossEvent().getBoss() == null) {
                    cancel();
                    return;
                }

                IronGolem boss = plugin.getBossEvent().getBoss();
                List<Player> nearbyPlayers = getNearbyPlayers(boss);

                // Controlla e attiva le abilità che sono pronte
                for (BossAbility ability : abilities) {
                    if (ability.isEnabled() && ability.canActivate()) {
                        ability.activate(boss, nearbyPlayers);
                    }
                }
            }
        }.runTaskTimer(plugin, 1, 1); // Controlla ogni tick
    }

    public void stopAbilityChecks() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }

        // Reset di tutte le abilità
        abilities.forEach(BossAbility::reset);
    }

    private List<Player> getNearbyPlayers(IronGolem boss) {
        double maxRange = plugin.getConfig().getDouble("abilities.max-detection-range", 20.0);
        List<Player> players = new ArrayList<>();

        for (Player player : boss.getWorld().getPlayers()) {
            if (player.getLocation().distance(boss.getLocation()) <= maxRange) {
                players.add(player);
            }
        }

        return players;
    }
}