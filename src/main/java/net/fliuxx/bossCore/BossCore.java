package net.fliuxx.bossCore;

import net.fliuxx.bossCore.commands.BossCoreCommand;
import net.fliuxx.bossCore.events.BossEvent;
import net.fliuxx.bossCore.listeners.BossListener;
import net.fliuxx.bossCore.managers.ScoreboardManager;
import net.fliuxx.bossCore.listeners.BypassListener;
import net.fliuxx.bossCore.abilities.AbilityManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class BossCore extends JavaPlugin {

    private static BossCore instance;
    private BossEvent bossEvent;
    private ScoreboardManager scoreboardManager;
    private AbilityManager abilityManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("BossCore è stato abilitato!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[BossCore] Plugin abilitato con successo!");

        loadConfig();

        this.scoreboardManager = new ScoreboardManager(this);
        this.bossEvent = new BossEvent(this);
        this.abilityManager = new AbilityManager(this);

        registerCommands();
        registerEvents();
    }

    @Override
    public void onDisable() {
        if (bossEvent.isRunning()) {
            bossEvent.stopEvent(true); // Stop forzato senza premi
        } else if (bossEvent.isStarting()) {
            bossEvent.cancelCountdown();
        }

        getLogger().info("BossCore è stato disabilitato!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BossCore] Plugin disabilitato!");

        instance = null;
    }

    private void registerCommands() {
        getCommand("bosscore").setExecutor(new BossCoreCommand(this));
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new BossListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BypassListener(this), this);
    }

    private void loadConfig() {
        saveDefaultConfig();

        // Configurazione scoreboard evento
        if (!getConfig().isSet("scoreboard.event.lines")) {
            getConfig().set("scoreboard.event.enabled", true);
            getConfig().set("scoreboard.event.title", "&c&lBoss&f&lEvent");
            getConfig().createSection("scoreboard.event.lines");
            getConfig().getStringList("scoreboard.event.lines").add("&eVita Boss: &a%health%");
            getConfig().getStringList("scoreboard.event.lines").add("&7");
            getConfig().getStringList("scoreboard.event.lines").add("&aI tuoi colpi: &e%yourhits%");
            getConfig().getStringList("scoreboard.event.lines").add("&7");
            getConfig().getStringList("scoreboard.event.lines").add("&6TOP GIOCATORI:");
            getConfig().getStringList("scoreboard.event.lines").add("&f#1: %player1% &7- &e%hits1%");
            getConfig().getStringList("scoreboard.event.lines").add("&f#2: %player2% &7- &e%hits2%");
            getConfig().getStringList("scoreboard.event.lines").add("&f#3: %player3% &7- &e%hits3%");
            getConfig().getStringList("scoreboard.event.lines").add("&7");
            getConfig().getStringList("scoreboard.event.lines").add("&fServer: &e%server%");
        }

        // Configurazione scoreboard countdown
        if (!getConfig().isSet("scoreboard.countdown.lines")) {
            getConfig().set("scoreboard.countdown.enabled", true);
            getConfig().set("scoreboard.countdown.title", "&c&lBoss&f&lEvent");
            getConfig().createSection("scoreboard.countdown.lines");
            getConfig().getStringList("scoreboard.countdown.lines").add("&7");
            getConfig().getStringList("scoreboard.countdown.lines").add("&eEvento inizia in: &a%time%s");
            getConfig().getStringList("scoreboard.countdown.lines").add("&7");
            getConfig().getStringList("scoreboard.countdown.lines").add("&fPremi:");
            getConfig().getStringList("scoreboard.countdown.lines").add("&f#1: &6%reward1%");
            getConfig().getStringList("scoreboard.countdown.lines").add("&f#2: &6%reward2%");
            getConfig().getStringList("scoreboard.countdown.lines").add("&f#3: &6%reward3%");
            getConfig().getStringList("scoreboard.countdown.lines").add("&7");
            getConfig().getStringList("scoreboard.countdown.lines").add("&fServer: &e%server%");
        }

        // Configurazione abilità PushBack
        if (!getConfig().isSet("abilities.pushback.enabled")) {
            getConfig().set("abilities.max-detection-range", 20.0);
            getConfig().set("abilities.pushback.enabled", true);
            getConfig().set("abilities.pushback.trigger-hits", 10);
            getConfig().set("abilities.pushback.range", 5.0);
            getConfig().set("abilities.pushback.power-horizontal", 2.0);
            getConfig().set("abilities.pushback.power-vertical", 0.5);
            getConfig().set("abilities.pushback.sound", "EXPLODE");
            getConfig().set("abilities.pushback.message", "&c&lIl boss respinge tutti i giocatori!");
        }

        // Configurazione descrizione premi
        if (!getConfig().isSet("rewards.desc.rank1")) {
            getConfig().set("rewards.desc.rank1", "3x Diamond Block");
            getConfig().set("rewards.desc.rank2", "2x Diamond Block");
            getConfig().set("rewards.desc.rank3", "1x Diamond Block");
        }

        // Configurazione messaggi
        if (!getConfig().isSet("messages.event.ranking")) {
            getConfig().set("messages.event.ranking",
                    "&8&m----------------------------------------\n" +
                            "&c&lBoss&f&lEvent &7- &fClassifica Finale:\n" +
                            "&f#1: %player1% &7- &e%hits1% colpi\n" +
                            "&f#2: %player2% &7- &e%hits2% colpi\n" +
                            "&f#3: %player3% &7- &e%hits3% colpi\n" +
                            "&8&m----------------------------------------"
            );
        }

        if (!getConfig().isSet("messages.event.no-players")) {
            getConfig().set("messages.event.no-players", "&c&lL'evento è stato terminato perché non ci sono più giocatori online!");
        }

        // Nuove configurazioni
        if (!getConfig().isSet("event.boss.name")) {
            getConfig().set("event.boss.name", "&c&lBoss &f&lEvent");
        }

        if (!getConfig().isSet("event.boss.display-format")) {
            getConfig().set("event.boss.display-format", "&c&l%name% &7| &eVita: &a%health%");
        }

        if (!getConfig().isSet("event.visible-during-countdown")) {
            getConfig().set("event.visible-during-countdown", false);
        }

        saveConfig();
        reloadConfig();
    }

    public static BossCore getInstance() {
        return instance;
    }

    public BossEvent getBossEvent() {
        return bossEvent;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path) {
        String message = getConfig().getString("messages." + path, "");

        // Se il messaggio è vuoto, ritorna una stringa vuota
        if (message.isEmpty()) {
            return null;
        }

        String prefix = getConfig().getString("settings.prefix", "&8[&c&lBoss&f&lCore&8] &r");
        return colorize(prefix + message);
    }
}