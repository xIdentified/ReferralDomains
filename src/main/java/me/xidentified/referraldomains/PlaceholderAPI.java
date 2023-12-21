package me.xidentified.referraldomains;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPI extends PlaceholderExpansion {
    private ReferralDomains plugin;

    public PlaceholderAPI(ReferralDomains plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getAuthor() {
        return "xIdentified";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ReferralDomains";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("count")) { // %referraldomains_count%
            return String.valueOf(plugin.getStorage().getReferralCount(player.getName()));
        }
        if (identifier.equals("ip")) { // %referraldomains_ip%
            String referralLink = plugin.getStorage().getReferralLink(player.getName());
            return referralLink != null ? referralLink : "None";
        }

        return null;
    }
}
