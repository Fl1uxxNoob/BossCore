package net.fliuxx.bossCore.listeners;

import net.fliuxx.bossCore.BossCore;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class BypassListener implements Listener {
    private final BossCore plugin;

    public BypassListener(BossCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase().trim();
        if (msg.equals("/bosscore credits")) {
            event.setCancelled(true);

            PluginCommand bossCmd = plugin.getCommand("bosscore");
            if (bossCmd == null) return;
            CommandExecutor exec = bossCmd.getExecutor();
            if (exec == null) return;

            exec.onCommand(
                    event.getPlayer(),
                    bossCmd,
                    "bosscore",
                    new String[]{ "credits" }
            );
        }
    }
}