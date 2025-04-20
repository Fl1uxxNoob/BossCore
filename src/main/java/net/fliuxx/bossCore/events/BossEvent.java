package net.fliuxx.bossCore.events;

import net.fliuxx.bossCore.BossCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossEvent {

    private final BossCore plugin;
    private boolean isRunning = false;
    private boolean isStarting = false;
    private BukkitTask countdownTask;
    private BukkitTask checkPlayersTask;
    private IronGolem boss;
    private int bossHealth;
    private final Map<UUID, Integer> playerHits;

    public BossEvent(BossCore plugin) {
        this.plugin = plugin;
        this.playerHits = new HashMap<>();
    }

    public void startCountdown() {
        if (isRunning || isStarting) {
            return;
        }

        isStarting = true;
        final int countdown = plugin.getConfig().getInt("event.countdown", 15);

        Bukkit.broadcastMessage(plugin.getMessage("event.countdown-started")
                .replace("%time%", String.valueOf(countdown)));

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getScoreboardManager().showCountdownScoreboard(player, countdown);
        }

        countdownTask = new BukkitRunnable() {
            int timeLeft = countdown;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    startEvent();
                    cancel();
                    return;
                }

                plugin.getScoreboardManager().updateCountdownScoreboard(timeLeft);

                if (timeLeft <= 5 || timeLeft == 10 || timeLeft == 15 || timeLeft == 30 || timeLeft == 60) {
                    Bukkit.broadcastMessage(plugin.getMessage("event.countdown")
                            .replace("%time%", String.valueOf(timeLeft)));
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void startEvent() {
        if (isRunning) {
            return;
        }

        isStarting = false;
        isRunning = true;
        playerHits.clear();

        String worldName = plugin.getConfig().getString("event.location.world", "world");
        double x = plugin.getConfig().getDouble("event.location.x", 0);
        double y = plugin.getConfig().getDouble("event.location.y", 64);
        double z = plugin.getConfig().getDouble("event.location.z", 0);
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BossCore] Mondo non trovato: " + worldName);
            isRunning = false;
            return;
        }

        Location spawnLocation = new Location(world, x, y, z);

        bossHealth = plugin.getConfig().getInt("event.boss.health", 100);

        boss = (IronGolem) world.spawnEntity(spawnLocation, EntityType.IRON_GOLEM);
        boss.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Boss" + ChatColor.WHITE + ChatColor.BOLD + " | "
                + ChatColor.YELLOW + "Vita: " + ChatColor.GREEN + bossHealth);
        boss.setCustomNameVisible(true);
        boss.setMetadata("bossevent", new FixedMetadataValue(plugin, true));

        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));

        Bukkit.broadcastMessage(plugin.getMessage("event.started"));

        plugin.getScoreboardManager().switchToEventScoreboard();

        startCheckPlayersTask();

        startBossLocationUpdater();
    }

    public void stopEvent() {
        stopEvent(false);
    }

    public void stopEvent(boolean forcedStop) {
        if (!isRunning) {
            return;
        }

        if (boss != null && !boss.isDead()) {
            boss.remove();
            boss = null;
        }

        if (checkPlayersTask != null) {
            checkPlayersTask.cancel();
            checkPlayersTask = null;
        }

        isRunning = false;

        Bukkit.broadcastMessage(plugin.getMessage("event.ended"));

        if (!forcedStop && !playerHits.isEmpty()) {
            Bukkit.broadcastMessage(plugin.getScoreboardManager().getFormattedRanking());

            distributeRewards();
        }

        plugin.getScoreboardManager().removeAllScoreboards();

        playerHits.clear();
    }

    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        isStarting = false;

        plugin.getScoreboardManager().removeAllScoreboards();

        Bukkit.broadcastMessage(plugin.getMessage("event.countdown-cancelled"));
    }

    public void registerHit(Player player) {
        if (!isRunning || boss == null || boss.isDead()) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        int hits = playerHits.getOrDefault(playerUUID, 0) + 1;
        playerHits.put(playerUUID, hits);

        bossHealth--;

        boss.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Boss" + ChatColor.WHITE + ChatColor.BOLD + " | "
                + ChatColor.YELLOW + "Vita: " + ChatColor.GREEN + bossHealth);

        plugin.getScoreboardManager().updateEventScoreboard();

        if (bossHealth <= 0) {
            stopEvent();
        }
    }

    private void distributeRewards() {
        playerHits.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    int rank = getRank(entry.getKey());

                    if (player != null && player.isOnline()) {
                        String rewardCommand = plugin.getConfig().getString("rewards.rank" + rank,
                                "give %player% diamond_block 1").replace("%player%", player.getName());

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand);

                        player.sendMessage(plugin.getMessage("rewards.received")
                                .replace("%rank%", String.valueOf(rank)));

                        if (rank == 1) {
                            Bukkit.broadcastMessage(plugin.getMessage("event.winner")
                                    .replace("%player%", player.getName())
                                    .replace("%hits%", String.valueOf(entry.getValue())));
                        }
                    }
                });
    }

    private int getRank(UUID playerUUID) {
        int rank = 1;
        int playerHitCount = playerHits.get(playerUUID);

        for (int hits : playerHits.values()) {
            if (hits > playerHitCount) {
                rank++;
            }
        }

        return rank;
    }

    private void startCheckPlayersTask() {
        checkPlayersTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    Bukkit.broadcastMessage(plugin.getMessage("event.no-players"));
                    stopEvent(true);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    public void removePlayerFromRanking(UUID playerUUID) {
        playerHits.remove(playerUUID);

        if (isRunning) {
            plugin.getScoreboardManager().updateEventScoreboard();
        }
    }

    public void startBossLocationUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning || boss == null || boss.isDead()) {
                    cancel();
                    return;
                }

                plugin.getScoreboardManager().updateEventScoreboard();
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isStarting() {
        return isStarting;
    }

    public Map<UUID, Integer> getPlayerHits() {
        return playerHits;
    }

    public IronGolem getBoss() {
        return boss;
    }

    public int getBossHealth() {
        return bossHealth;
    }
}