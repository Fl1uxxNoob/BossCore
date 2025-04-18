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

        // Annuncia l'inizio del countdown
        Bukkit.broadcastMessage(plugin.getMessage("event.countdown-started")
                .replace("%time%", String.valueOf(countdown)));

        // Mostra la scoreboard del countdown a tutti i giocatori
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

                // Aggiorna la scoreboard del countdown
                plugin.getScoreboardManager().updateCountdownScoreboard(timeLeft);

                if (timeLeft <= 5 || timeLeft == 10 || timeLeft == 15 || timeLeft == 30 || timeLeft == 60) {
                    Bukkit.broadcastMessage(plugin.getMessage("event.countdown")
                            .replace("%time%", String.valueOf(timeLeft)));
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20); // 20 ticks = 1 secondo
    }

    public void startEvent() {
        if (isRunning) {
            return;
        }

        isStarting = false;
        isRunning = true;
        playerHits.clear();

        // Carica i valori dalla config
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

        // Carica la vita del boss
        bossHealth = plugin.getConfig().getInt("event.boss.health", 100);

        // Spawna il boss
        boss = (IronGolem) world.spawnEntity(spawnLocation, EntityType.IRON_GOLEM);
        boss.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Boss" + ChatColor.WHITE + ChatColor.BOLD + " | "
                + ChatColor.YELLOW + "Vita: " + ChatColor.GREEN + bossHealth);
        boss.setCustomNameVisible(true);
        boss.setMetadata("bossevent", new FixedMetadataValue(plugin, true));

        // Disabilita il movimento e l'attacco del boss
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 100, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));

        // Annuncia l'inizio dell'evento
        Bukkit.broadcastMessage(plugin.getMessage("event.started"));

        // Cambia la scoreboard per tutti i giocatori
        plugin.getScoreboardManager().switchToEventScoreboard();

        // Avvia il task per controllare se ci sono ancora giocatori online
        startCheckPlayersTask();
    }

    public void stopEvent() {
        stopEvent(false);
    }

    public void stopEvent(boolean forcedStop) {
        if (!isRunning) {
            return;
        }

        // Rimuovi il boss se esiste
        if (boss != null && !boss.isDead()) {
            boss.remove();
            boss = null;
        }

        // Ferma il task di controllo dei giocatori
        if (checkPlayersTask != null) {
            checkPlayersTask.cancel();
            checkPlayersTask = null;
        }

        isRunning = false;

        // Annuncia la fine dell'evento
        Bukkit.broadcastMessage(plugin.getMessage("event.ended"));

        // Mostra la classifica finale solo se non è uno stop forzato
        if (!forcedStop && !playerHits.isEmpty()) {
            Bukkit.broadcastMessage(plugin.getScoreboardManager().getFormattedRanking());

            // Distribuisci i premi solo se non è uno stop forzato
            distributeRewards();
        }

        // Rimuovi le scoreboard
        plugin.getScoreboardManager().removeAllScoreboards();

        // Pulisci i dati
        playerHits.clear();
    }

    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        isStarting = false;

        // Rimuovi le scoreboard del countdown
        plugin.getScoreboardManager().removeAllScoreboards();

        Bukkit.broadcastMessage(plugin.getMessage("event.countdown-cancelled"));
    }

    public void registerHit(Player player) {
        if (!isRunning || boss == null || boss.isDead()) {
            return;
        }

        // Aggiungi l'hit al giocatore
        UUID playerUUID = player.getUniqueId();
        int hits = playerHits.getOrDefault(playerUUID, 0) + 1;
        playerHits.put(playerUUID, hits);

        // Riduci la vita del boss
        bossHealth--;

        // Aggiorna il nome del boss
        boss.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "Boss" + ChatColor.WHITE + ChatColor.BOLD + " | "
                + ChatColor.YELLOW + "Vita: " + ChatColor.GREEN + bossHealth);

        // Aggiorna la scoreboard
        plugin.getScoreboardManager().updateEventScoreboard();

        // Controlla se il boss è morto
        if (bossHealth <= 0) {
            stopEvent();
        }
    }

    private void distributeRewards() {
        // Ordina i giocatori per hit
        playerHits.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3) // Solo i primi 3
                .forEach(entry -> {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    int rank = getRank(entry.getKey());

                    if (player != null && player.isOnline()) {
                        // Esegui il comando di premio
                        String rewardCommand = plugin.getConfig().getString("rewards.rank" + rank,
                                "give %player% diamond_block 1").replace("%player%", player.getName());

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand);

                        // Notifica il giocatore
                        player.sendMessage(plugin.getMessage("rewards.received")
                                .replace("%rank%", String.valueOf(rank)));

                        // Annuncia il vincitore se è il primo posto
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
                // Se non ci sono giocatori online, termina l'evento
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    Bukkit.broadcastMessage(plugin.getMessage("event.no-players"));
                    stopEvent(true); // Termina forzatamente
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20, 20); // Controlla ogni secondo
    }

    public void removePlayerFromRanking(UUID playerUUID) {
        playerHits.remove(playerUUID);

        // Se l'evento è in corso, aggiorna la scoreboard
        if (isRunning) {
            plugin.getScoreboardManager().updateEventScoreboard();
        }
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