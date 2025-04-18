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

    public ScoreboardManager(BossCore plugin) {
        this.plugin = plugin;
    }

    public void showScoreboard(Player player) {
        // Cambiato il modo in cui otteniamo l'oggetto scoreboard
        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = bukkitManager.getNewScoreboard();

        // In Spigot 1.8.9 registerNewObjective() accetta solo 2 parametri
        Objective objective = scoreboard.registerNewObjective("bossevent", "dummy");
        objective.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Boss" + ChatColor.WHITE + "" + ChatColor.BOLD + "Event");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Placeholder scores
        objective.getScore(ChatColor.YELLOW + "Boss HP: " + ChatColor.GREEN + plugin.getBossEvent().getBossHealth()).setScore(15);
        objective.getScore(" ").setScore(14);
        objective.getScore(ChatColor.GOLD + "TOP HITTERS:").setScore(13);
        objective.getScore("  ").setScore(9);
        objective.getScore(ChatColor.GRAY + "Server: " + ChatColor.WHITE + Bukkit.getServerName()).setScore(8);

        player.setScoreboard(scoreboard);
    }

    public void updateScoreboard() {
        Map<UUID, Integer> playerHits = plugin.getBossEvent().getPlayerHits();

        // Ottieni i top 3 players
        List<Map.Entry<UUID, Integer>> topPlayers = playerHits.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = player.getScoreboard();

            if (scoreboard == null || scoreboard.getObjective("bossevent") == null) {
                showScoreboard(player);
                scoreboard = player.getScoreboard();
            }

            Objective objective = scoreboard.getObjective("bossevent");

            // Aggiorna la vita del boss
            for (String entry : new ArrayList<>(scoreboard.getEntries())) {
                if (entry.startsWith(ChatColor.YELLOW + "Boss HP: ")) {
                    scoreboard.resetScores(entry);
                }
            }
            objective.getScore(ChatColor.YELLOW + "Boss HP: " + ChatColor.GREEN + plugin.getBossEvent().getBossHealth()).setScore(15);

            // Rimuovi i vecchi punteggi
            for (String entry : new ArrayList<>(scoreboard.getEntries())) {
                if (entry.startsWith(ChatColor.GOLD + "#") ||
                        entry.equals("   ") ||
                        entry.equals("    ") ||
                        entry.equals("     ")) {
                    scoreboard.resetScores(entry);
                }
            }

            // Aggiungi i nuovi punteggi
            int score = 12;
            for (int i = 0; i < topPlayers.size(); i++) {
                Map.Entry<UUID, Integer> entry = topPlayers.get(i);
                String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (playerName == null) playerName = "Unknown";

                objective.getScore(ChatColor.GOLD + "#" + (i + 1) + " " +
                        ChatColor.WHITE + playerName + ": " +
                        ChatColor.YELLOW + entry.getValue()).setScore(score--);
            }

            // Riempi i posti vuoti
            for (int i = topPlayers.size(); i < 3; i++) {
                objective.getScore(ChatColor.GOLD + "#" + (i + 1) + " " + ChatColor.GRAY + "Nessuno").setScore(score--);
            }
        }
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}