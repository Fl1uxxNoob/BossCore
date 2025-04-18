package net.fliuxx.bossCore.commands;

import net.fliuxx.bossCore.BossCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BossCoreCommand implements CommandExecutor {

    private final BossCore plugin;

    public BossCoreCommand(BossCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("commands.help"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (!sender.hasPermission("bosscore.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }

                if (plugin.getBossEvent().isRunning()) {
                    sender.sendMessage(plugin.getMessage("event.already-running"));
                    return true;
                }

                if (plugin.getBossEvent().isStarting()) {
                    sender.sendMessage(plugin.getMessage("event.already-starting"));
                    return true;
                }

                plugin.getBossEvent().startCountdown();
                sender.sendMessage(plugin.getMessage("event.starting"));
                return true;

            case "stop":
                if (!sender.hasPermission("bosscore.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }

                if (!plugin.getBossEvent().isRunning()) {
                    if (plugin.getBossEvent().isStarting()) {
                        plugin.getBossEvent().cancelCountdown();
                        sender.sendMessage(plugin.getMessage("event.countdown-stopped"));
                    } else {
                        sender.sendMessage(plugin.getMessage("event.not-running"));
                    }
                    return true;
                }

                // Stoppa l'evento con il flag di stop forzato
                plugin.getBossEvent().stopEvent(true);
                sender.sendMessage(plugin.getMessage("event.stopped"));
                return true;

            case "reload":
                if (!sender.hasPermission("bosscore.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }

                plugin.reloadConfig();
                sender.sendMessage(plugin.getMessage("reload"));
                return true;

            case "setlocation":
                if (!sender.hasPermission("bosscore.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessage("player-only"));
                    return true;
                }

                Player player = (Player) sender;
                plugin.getConfig().set("event.location.world", player.getWorld().getName());
                plugin.getConfig().set("event.location.x", player.getLocation().getX());
                plugin.getConfig().set("event.location.y", player.getLocation().getY());
                plugin.getConfig().set("event.location.z", player.getLocation().getZ());
                plugin.saveConfig();

                sender.sendMessage(plugin.getMessage("location-set"));
                return true;

            default:
                sender.sendMessage(plugin.getMessage("commands.help"));
                return true;
        }
    }
}