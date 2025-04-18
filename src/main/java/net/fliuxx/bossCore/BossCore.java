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

        // Inizializza i manager
        this.scoreboardManager = new ScoreboardManager(this);
        this.bossEvent = new BossEvent(this);

        // Registra i comandi e gli eventi
        registerCommands();
        registerEvents();

        // Carica la configurazione
        loadConfig();
    }

    @Override
    public void onDisable() {
        // Se l'evento è in corso, terminalo
        if (bossEvent.isRunning()) {
            bossEvent.stopEvent();
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