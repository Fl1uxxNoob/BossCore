package net.fliuxx.bossCore;

import net.fliuxx.bossCore.commands.BossCoreCommand;
import net.fliuxx.bossCore.events.BossEvent;
import net.fliuxx.bossCore.listeners.BossListener;
import net.fliuxx.bossCore.managers.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class BossCore extends JavaPlugin {

    private static BossCore instance;
    private BossEvent bossEvent;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        // Salva l'istanza per accesso statico
        instance = this;

        // Plugin startup logic
        getLogger().info("BossCore è stato abilitato!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[BossCore] Plugin abilitato con successo!");

        // Carica la configurazione prima di inizializzare i manager
        loadConfig();

        // Inizializza i manager
        this.scoreboardManager = new ScoreboardManager(this);
        this.bossEvent = new BossEvent(this);

        // Registra i comandi e gli eventi
        registerCommands();
        registerEvents();
    }

    @Override
    public void onDisable() {
        // Se l'evento è in corso, terminalo
        if (bossEvent.isRunning()) {
            bossEvent.stopEvent(true); // Stop forzato senza premi
        } else if (bossEvent.isStarting()) {
            bossEvent.cancelCountdown();
        }

        // Plugin shutdown logic
        getLogger().info("BossCore è stato disabilitato!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BossCore] Plugin disabilitato!");

        // Annulla il riferimento all'istanza
        instance = null;
    }

    private void registerCommands() {
        // Registra il comando principale
        getCommand("bosscore").setExecutor(new BossCoreCommand(this));
    }

    private void registerEvents() {
        // Registra il listener per il boss
        Bukkit.getPluginManager().registerEvents(new BossListener(this), this);
    }

    private void loadConfig() {
        // Crea la configurazione predefinita se non esiste
        saveDefaultConfig();

        // Aggiungi valori di default per le scoreboard se non esistono
        if (!getConfig().isSet("scoreboard.event.lines")) {
            getConfig().set("scoreboard.event.enabled", true);
            getConfig().set("scoreboard.event.title", "&c&lBoss&f&lEvent");
            getConfig().createSection("scoreboard.event.lines");
            getConfig().getStringList("scoreboard.event.lines").add("&eVita Boss: &a%health%");
            getConfig().getStringList("scoreboard.event.lines").add("&7");
            getConfig().getStringList("scoreboard.event.lines").add("&6TOP GIOCATORI:");
            getConfig().getStringList("scoreboard.event.lines").add("&f#1: %player1% &7- &e%hits1%");
            getConfig().getStringList("scoreboard.event.lines").add("&f#2: %player2% &7- &e%hits2%");
            getConfig().getStringList("scoreboard.event.lines").add("&f#3: %player3% &7- &e%hits3%");
            getConfig().getStringList("scoreboard.event.lines").add("&7");
            getConfig().getStringList("scoreboard.event.lines").add("&fServer: &e%server%");
        }

        if (!getConfig().isSet("scoreboard.countdown.lines")) {
            getConfig().set("scoreboard.countdown.enabled", true);
            getConfig().set("scoreboard.countdown.title", "&c&lBoss&f&lEvent &7- &fCountdown");
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

        if (!getConfig().isSet("rewards.desc.rank1")) {
            getConfig().set("rewards.desc.rank1", "3x Diamond Block");
            getConfig().set("rewards.desc.rank2", "2x Diamond Block");
            getConfig().set("rewards.desc.rank3", "1x Diamond Block");
        }

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

        // Salva le impostazioni predefinite
        saveConfig();

        // Carica la configurazione
        reloadConfig();
    }

    // Getter per accesso agli oggetti principali
    public static BossCore getInstance() {
        return instance;
    }

    public BossEvent getBossEvent() {
        return bossEvent;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    // Metodo di utilità per colorare le stringhe
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Metodo per ottenere un messaggio dalla config
    public String getMessage(String path) {
        String prefix = getConfig().getString("settings.prefix", "&8[&c&lBoss&f&lCore&8] &r");
        String message = getConfig().getString("messages." + path, "&cMessaggio non trovato: " + path);
        return colorize(prefix + message);
    }
}