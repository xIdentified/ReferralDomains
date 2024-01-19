package me.xidentified.referraldomains;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
        plugin.startTrackingPlayer(playerUUID);

        // Check if the player is joining for the first time
        if (!plugin.getStorage().hasPlayerJoinedBefore(playerUUID)) {
            // Mark the player as having joined
            plugin.getStorage().markPlayerJoined(playerUUID);

            // Get the domain the player joined with
            String hostname = event.getHostname();
            String domain = hostname.split(":")[0];
            plugin.debugLog(player.getName() + " joined through the domain: " + domain);

            // Handle referral logic if the player joined through a referral link
            if (plugin.isReferralDomain(domain)) {
                plugin.debugLog(domain + " was a valid referral domain");
                plugin.handleReferral(player.getName(), domain);
            }
        }
    }

    @EventHandler
    public void afterPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Save player IP to prevent abuse
        String ip = player.getAddress().getAddress().getHostAddress();
        plugin.getStorage().savePlayerIP(player.getUniqueId(), ip);

        // Executes pending rewards after the player logs in
        plugin.grantPendingRewards(player.getName());
    }
}
