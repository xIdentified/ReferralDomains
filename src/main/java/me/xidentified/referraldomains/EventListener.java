package me.xidentified.referraldomains;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class EventListener implements Listener {

    private final ReferralDomains plugin;

    public EventListener(ReferralDomains plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if the player is joining for the first time
        if (!plugin.getStorage().hasPlayerJoinedBefore(playerUUID)) {
            // Mark the player as having joined
            plugin.getStorage().markPlayerJoined(playerUUID);

            String hostname = event.getHostname();
            String domain = hostname.split(":")[0]; // Remove port number if present
            plugin.debugLog(player.getName() + " joined through the domain: " + domain);

            // Handle the referral if the player joined through a referral link
            if (plugin.isReferralDomain(domain)) {
                plugin.debugLog(domain + " was a valid referral domain");
                plugin.handleReferral(player.getName(), domain);
            }
        } else {
            // Player has joined before, so no referral handling is needed
            plugin.debugLog(player.getName() + " has joined before.");
        }

        // Grant any pending rewards for the referrer
        plugin.grantPendingRewards(player.getName());
        plugin.startTrackingPlayer(playerUUID);
    }

}
