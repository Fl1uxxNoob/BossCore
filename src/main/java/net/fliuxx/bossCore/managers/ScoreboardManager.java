package net.fliuxx.bossCore.managers;

import net.fliuxx.bossCore.BossCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.Collectors;

public class ScoreboardManager {

    private final BossCore plugin;
    private final Map<UUID, ScoreboardType> playerScoreboards;

    public enum ScoreboardType {
        EVENT,
        COUNTDOWN,
        NONE
    }

    public ScoreboardManager(BossCore plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
    }

    public void showEventScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.event.enabled", true)) {
            return;
        }

        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = bukkitManager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("bossevent", "dummy");
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("scoreboard.event.title", "&c&lBossEvent")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Creazione della scoreboard dall'elenco di righe nella config
        List<String> lines = plugin.getConfig().getStringList("scoreboard.event.lines");
        int scoreValue = lines.size();

        for (String line : lines) {
            // Sostituisci i placeholder
            line = replacePlaceholders(line, ScoreboardType.EVENT);
            String entry = ChatColor.translateAlternateColorCodes('&', line);

            // Evitare entries duplicate
            while (scoreboard.getEntries().contains(entry)) {
                entry = entry + "§r";
            }

            // NUOVA LOGICA: tronca a 40 caratteri
            if (entry.length() > 40) {
                entry = entry.substring(0, 40);
            }

            objective.getScore(entry).setScore(scoreValue--);
        }

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), ScoreboardType.EVENT);
    }

    public void showCountdownScoreboard(Player player, int timeLeft) {
        if (!plugin.getConfig().getBoolean("scoreboard.countdown.enabled", true)) {
            return;
        }

        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = bukkitManager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective("bosscountdown", "dummy");
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("scoreboard.countdown.title", "&c&lBossEvent &7- &fCountdown")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Creazione della scoreboard dall'elenco di righe nella config
        List<String> lines = plugin.getConfig().getStringList("scoreboard.countdown.lines");
        int scoreValue = lines.size();

        for (String line : lines) {
            // Sostituisci i placeholder incluso il tempo rimanente
            line = line.replace("%time%", String.valueOf(timeLeft));
            line = replacePlaceholders(line, ScoreboardType.COUNTDOWN);
            String entry = ChatColor.translateAlternateColorCodes('&', line);

            // Evitare entries duplicate
            while (scoreboard.getEntries().contains(entry)) {
                entry = entry + "§r";
            }

            // NUOVA LOGICA: tronca a 40 caratteri
            if (entry.length() > 40) {
                entry = entry.substring(0, 40);
            }

            objective.getScore(entry).setScore(scoreValue--);
        }

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), ScoreboardType.COUNTDOWN);
    }

    private String replacePlaceholders(String line, ScoreboardType type) {
        // Placeholders comuni
        line = line.replace("%server%", Bukkit.getServerName());

        // Reward descriptions
        line = line.replace("%reward1%", plugin.getConfig().getString("rewards.desc.rank1", "3x Diamond Block"));
        line = line.replace("%reward2%", plugin.getConfig().getString("rewards.desc.rank2", "2x Diamond Block"));
        line = line.replace("%reward3%", plugin.getConfig().getString("rewards.desc.rank3", "1x Diamond Block"));

        if (type == ScoreboardType.EVENT) {
            // Placeholders specifici dell'evento
            line = line.replace("%health%", String.valueOf(plugin.getBossEvent().getBossHealth()));

            // Top player placeholders
            Map<UUID, Integer> playerHits = plugin.getBossEvent().getPlayerHits();
            List<Map.Entry<UUID, Integer>> topPlayers = playerHits.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            for (int i = 0; i < 3; i++) {
                String playerName = "Nessuno";
                String hits = "0";

                if (i < topPlayers.size()) {
                    Map.Entry<UUID, Integer> entry = topPlayers.get(i);
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    playerName = (name != null) ? name : "Unknown";
                    hits = String.valueOf(entry.getValue());
                }

                line = line.replace("%player" + (i+1) + "%", playerName);
                line = line.replace("%hits" + (i+1) + "%", hits);
            }
        }

        return line;
    }

    public void updateEventScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (playerScoreboards.getOrDefault(uuid, ScoreboardType.NONE) == ScoreboardType.EVENT) {
                showEventScoreboard(player);
            }
        }
    }

    public void updateCountdownScoreboard(int timeLeft) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (playerScoreboards.getOrDefault(uuid, ScoreboardType.NONE) == ScoreboardType.COUNTDOWN) {
                showCountdownScoreboard(player, timeLeft);
            }
        }
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        playerScoreboards.remove(player.getUniqueId());
    }

    public void removeAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeScoreboard(player);
        }
        playerScoreboards.clear();
    }

    public ScoreboardType getPlayerScoreboardType(UUID uuid) {
        return playerScoreboards.getOrDefault(uuid, ScoreboardType.NONE);
    }

    public void switchToEventScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showEventScoreboard(player);
        }
    }

    public String getFormattedRanking() {
        Map<UUID, Integer> playerHits = plugin.getBossEvent().getPlayerHits();
        List<Map.Entry<UUID, Integer>> topPlayers = playerHits.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        String ranking = plugin.getMessage("event.ranking");

        for (int i = 0; i < 3; i++) {
            String playerName = "Nessuno";
            String hits = "0";

            if (i < topPlayers.size()) {
                Map.Entry<UUID, Integer> entry = topPlayers.get(i);
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                playerName = (name != null) ? name : "Unknown";
                hits = String.valueOf(entry.getValue());
            }

            ranking = ranking.replace("%player" + (i+1) + "%", playerName);
            ranking = ranking.replace("%hits" + (i+1) + "%", hits);
        }

        return ranking;
    }
}