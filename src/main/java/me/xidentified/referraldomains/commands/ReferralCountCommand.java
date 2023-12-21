package me.xidentified.referraldomains.commands;

import me.xidentified.referraldomains.ReferralDomains;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReferralCountCommand implements CommandExecutor {

    private final ReferralDomains plugin;

    public ReferralCountCommand(ReferralDomains plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        int count = plugin.getStorage().getReferralCount(player.getName().toLowerCase());

        player.sendMessage(ChatColor.YELLOW + "You have referred " + count + " player(s).");
        return true;
    }
}
