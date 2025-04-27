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
    private Location bossSpawnLocation;

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

        // Preparare la location per lo spawn del boss
        String worldName = plugin.getConfig().getString("event.location.world", "world");
        double x = plugin.getConfig().getDouble("event.location.x", 0);
        double y = plugin.getConfig().getDouble("event.location.y", 64);
        double z = plugin.getConfig().getDouble("event.location.z", 0);
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BossCore] Mondo non trovato: " + worldName);
            isStarting = false;
            return;
        }

        bossSpawnLocation = new Location(world, x, y, z);
        bossHealth = plugin.getConfig().getInt("event.boss.health", 100);

        // Controllo se il boss deve essere visibile durante il countdown
        if (plugin.getConfig().getBoolean("event.visible-during-countdown", false)) {
            spawnBoss(false); // Spawna boss (non attaccabile)
        }

        String countdownMessage = plugin.getMessage("event.countdown-started")
                .replace("%time%", String.valueOf(countdown));

        if (!countdownMessage.isEmpty()) {
            Bukkit.broadcastMessage(countdownMessage);
        }

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
                    String message = plugin.getMessage("event.countdown")
                            .replace("%time%", String.valueOf(timeLeft));

                    if (!message.isEmpty()) {
                        Bukkit.broadcastMessage(message);
                    }
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void spawnBoss(boolean attackable) {
        if (boss != null && !boss.isDead()) {
            boss.remove();
        }

        boss = (IronGolem) bossSpawnLocation.getWorld().spawnEntity(bossSpawnLocation, EntityType.IRON_GOLEM);

        // Ottenere il nome del boss dal config
        String bossName = plugin.getConfig().getString("event.boss.name", "&c&lBoss &f&lEvent");
        String displayFormat = plugin.getConfig().getString("event.boss.display-format", "&c&l%name% &7| &eVita: &a%health%");

        String customName = displayFormat.replace("%name%", bossName).replace("%health%", String.valueOf(bossHealth));
        boss.setCustomName(ChatColor.translateAlternateColorCodes('&', customName));
        boss.setCustomNameVisible(true);

        // Metadati per identificare il boss dell'evento
        boss.setMetadata("bossevent", new FixedMetadataValue(plugin, true));

        // Se il boss non è attaccabile (durante il countdown), aggiungi un metadata per indicarlo
        if (!attackable) {
            boss.setMetadata("countdown", new FixedMetadataValue(plugin, true));
        } else {
            boss.removeMetadata("countdown", plugin);
        }

        // Aggiungere effetti per impedire al boss di muoversi
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));
    }

    public void startEvent() {
        if (isRunning) {
            return;
        }

        isStarting = false;
        isRunning = true;
        playerHits.clear();

        // Se il boss è già visibile (durante il countdown), aggiornalo per renderlo attaccabile
        if (boss != null && !boss.isDead()) {
            boss.removeMetadata("countdown", plugin);
        } else {
            // Altrimenti, spawnalo
            spawnBoss(true);
        }

        String startMessage = plugin.getMessage("event.started");
        if (!startMessage.isEmpty()) {
            Bukkit.broadcastMessage(startMessage);
        }

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

        String endMessage = plugin.getMessage("event.ended");
        if (!endMessage.isEmpty()) {
            Bukkit.broadcastMessage(endMessage);
        }

        if (!forcedStop && !playerHits.isEmpty()) {
            String ranking = plugin.getScoreboardManager().getFormattedRanking();
            if (!ranking.isEmpty()) {
                Bukkit.broadcastMessage(ranking);
            }

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

        // Se il boss è visibile durante il countdown, rimuovilo
        if (boss != null && !boss.isDead() && boss.hasMetadata("countdown")) {
            boss.remove();
            boss = null;
        }

        plugin.getScoreboardManager().removeAllScoreboards();

        String cancelMessage = plugin.getMessage("event.countdown-cancelled");
        if (!cancelMessage.isEmpty()) {
            Bukkit.broadcastMessage(cancelMessage);
        }
    }

    public void registerHit(Player player) {
        if (!isRunning || boss == null || boss.isDead() || boss.hasMetadata("countdown")) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        int hits = playerHits.getOrDefault(playerUUID, 0) + 1;
        playerHits.put(playerUUID, hits);

        bossHealth--;

        // Aggiornare il nome del boss con la nuova vita
        String bossName = plugin.getConfig().getString("event.boss.name", "&c&lBoss &f&lEvent");
        String displayFormat = plugin.getConfig().getString("event.boss.display-format", "&c&l%name% &7| &eVita: &a%health%");

        String customName = displayFormat.replace("%name%", bossName).replace("%health%", String.valueOf(bossHealth));
        boss.setCustomName(ChatColor.translateAlternateColorCodes('&', customName));

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

                        String rewardMessage = plugin.getMessage("rewards.received")
                                .replace("%rank%", String.valueOf(rank));

                        if (!rewardMessage.isEmpty()) {
                            player.sendMessage(rewardMessage);
                        }

                        if (rank == 1) {
                            String winnerMessage = plugin.getMessage("event.winner")
                                    .replace("%player%", player.getName())
                                    .replace("%hits%", String.valueOf(entry.getValue()));

                            if (!winnerMessage.isEmpty()) {
                                Bukkit.broadcastMessage(winnerMessage);
                            }
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
                    String noPlayersMessage = plugin.getMessage("event.no-players");
                    if (!noPlayersMessage.isEmpty()) {
                        Bukkit.broadcastMessage(noPlayersMessage);
                    }
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