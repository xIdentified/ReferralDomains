package me.xidentified.referraldomains;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class EventListener implements Listener {

    private final ReferralDomains plugin;

    public EventListener(ReferralDomains plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        String hostname = event.getHostname();

        // Grant the referrer's pending rewards
        Player player = event.getPlayer();
        plugin.grantPendingRewards(player.getName());

        plugin.startTrackingPlayer(player.getUniqueId());

        // Get the domain the player joined through
        String domain = hostname.split(":")[0]; // This splits off the port number if present
        plugin.getLogger().warning(event.getPlayer() + " joined through the domain: " + domain);

        // Check if the player joined through a referral link
        if (plugin.isReferralDomain(domain)) {
            // Handle the referral logic here - giving rewards etc
            plugin.debugLog(domain + " was a valid referral domain");
            plugin.handleReferral(event.getPlayer().getName(), domain);
        }
    }

}
