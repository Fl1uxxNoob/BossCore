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
    private final static HashMap<UUID, ScoreboardManager> playerManagers = new HashMap<>();

    private final Scoreboard scoreboard;
    private final Objective sidebar;

    public enum ScoreboardType {
        EVENT,
        COUNTDOWN,
        NONE
    }

    public ScoreboardManager(BossCore plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
        this.scoreboard = null;
        this.sidebar = null;
    }

    private ScoreboardManager(Player player) {
        this.plugin = BossCore.getInstance();
        this.playerScoreboards = new HashMap<>();

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.sidebar = this.scoreboard.registerNewObjective("bosscore", "dummy");
        this.sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Registra 15 team per gestire le righe della scoreboard
        for (int i = 0; i <= 14; i++) {
            Team team = this.scoreboard.registerNewTeam("BOSSCORE_" + i);
            team.addEntry(genEntry(i));
        }

        player.setScoreboard(this.scoreboard);
        playerManagers.put(player.getUniqueId(), this);
    }

    public static ScoreboardManager getByPlayer(Player player) {
        return playerManagers.get(player.getUniqueId());
    }

    public static ScoreboardManager createScore(Player player) {
        return new ScoreboardManager(player);
    }

    public static ScoreboardManager removeScore(Player player) {
        return playerManagers.remove(player.getUniqueId());
    }

    public void setTitle(String title) {
        title = BossCore.colorize(title);
        this.sidebar.setDisplayName(title.length() > 32 ? title.substring(0, 32) : title);
    }

    public void setSlot(int slot, String text) {
        Team team = this.scoreboard.getTeam("BOSSCORE_" + slot);
        if (team == null) return;

        String entry = genEntry(slot);
        if (!this.scoreboard.getEntries().contains(entry)) {
            this.sidebar.getScore(entry).setScore(slot);
        }

        text = BossCore.colorize(text);
        String pre = getFirstSplit(text);
        String suf = getFirstSplit(ChatColor.getLastColors(pre) + getSecondSplit(text));
        team.setPrefix(pre);
        team.setSuffix(suf);
    }

    public void removeSlot(int slot) {
        String entry = genEntry(slot);
        if (this.scoreboard.getEntries().contains(entry)) {
            this.scoreboard.resetScores(entry);
        }
    }

    public void setSlotsFromList(List<String> list) {
        while (list.size() > 15) {
            list.remove(list.size() - 1);
        }

        int slot = list.size();
        if (slot < 15) {
            for (int i = slot + 1; i <= 15; i++) {
                removeSlot(i);
            }
        }

        for (String line : list) {
            setSlot(slot, line);
            slot--;
        }
    }

    public void showEventScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.event.enabled", true)) {
            return;
        }

        ScoreboardManager board = getByPlayer(player);
        if (board == null) {
            board = createScore(player);
        } else {
            player.setScoreboard(board.scoreboard);
        }

        board.setTitle(plugin.getConfig().getString("scoreboard.event.title", "&c&lBossEvent"));

        List<String> lines = plugin.getConfig().getStringList("scoreboard.event.lines");
        List<String> processed = new ArrayList<>();

        for (String line : lines) {
            processed.add(replacePlaceholders(line, ScoreboardType.EVENT, player));
        }

        board.setSlotsFromList(processed);
        playerScoreboards.put(player.getUniqueId(), ScoreboardType.EVENT);
    }

    public void showCountdownScoreboard(Player player, int timeLeft) {
        if (!plugin.getConfig().getBoolean("scoreboard.countdown.enabled", true)) {
            return;
        }

        ScoreboardManager board = getByPlayer(player);
        if (board == null) {
            board = createScore(player);
        }

        // Imposta il titolo
        board.setTitle(plugin.getConfig().getString("scoreboard.countdown.title", "&c&lBossEvent"));

        // Ottieni e elabora le righe
        List<String> lines = plugin.getConfig().getStringList("scoreboard.countdown.lines");
        List<String> processed = new ArrayList<>();

        for (String line : lines) {
            line = line.replace("%time%", String.valueOf(timeLeft));
            processed.add(replacePlaceholders(line, ScoreboardType.COUNTDOWN, player));
        }

        board.setSlotsFromList(processed);
        playerScoreboards.put(player.getUniqueId(), ScoreboardType.COUNTDOWN);
    }

    private String replacePlaceholders(String line, ScoreboardType type, Player player) {
        line = line.replace("%server%", Bukkit.getServerName());

        line = line.replace("%reward1%", plugin.getConfig().getString("rewards.desc.rank1", "3x Diamond Block"));
        line = line.replace("%reward2%", plugin.getConfig().getString("rewards.desc.rank2", "2x Diamond Block"));
        line = line.replace("%reward3%", plugin.getConfig().getString("rewards.desc.rank3", "1x Diamond Block"));

        if (type == ScoreboardType.EVENT) {
            line = line.replace("%health%", String.valueOf(plugin.getBossEvent().getBossHealth()));

            // Aggiungi le coordinate del boss
            if (plugin.getBossEvent().getBoss() != null) {
                line = line.replace("%boss_x%", String.format("%.1f", plugin.getBossEvent().getBoss().getLocation().getX()));
                line = line.replace("%boss_y%", String.format("%.1f", plugin.getBossEvent().getBoss().getLocation().getY()));
                line = line.replace("%boss_z%", String.format("%.1f", plugin.getBossEvent().getBoss().getLocation().getZ()));
                line = line.replace("%boss_world%", plugin.getBossEvent().getBoss().getWorld().getName());
            } else {
                line = line.replace("%boss_x%", "N/A");
                line = line.replace("%boss_y%", "N/A");
                line = line.replace("%boss_z%", "N/A");
                line = line.replace("%boss_world%", "N/A");
            }

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

            if (player != null && line.contains("%yourhits%")) {
                int hits = playerHits.getOrDefault(player.getUniqueId(), 0);
                line = line.replace("%yourhits%", String.valueOf(hits));
            } else {
                line = line.replace("%yourhits%", "0");
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
        ScoreboardManager board = getByPlayer(player);
        if (board != null) {
            removeScore(player);
            Scoreboard nullBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(nullBoard);
        }
        playerScoreboards.remove(player.getUniqueId());
    }

    public void removeAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeScoreboard(player);
        }
        playerManagers.clear();
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

    private String genEntry(int slot) {
        return ChatColor.values()[slot].toString();
    }

    private String getFirstSplit(String s) {
        return (s.length() > 16) ? s.substring(0, 16) : s;
    }

    private String getSecondSplit(String s) {
        if (s.length() > 32)
            s = s.substring(0, 32);
        return (s.length() > 16) ? s.substring(16) : "";
    }
}