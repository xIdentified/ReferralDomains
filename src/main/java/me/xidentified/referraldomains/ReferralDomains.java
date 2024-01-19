package me.xidentified.referraldomains;

import me.xidentified.referraldomains.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public final class ReferralDomains extends JavaPlugin {
    public Map<String, String> referralLinks; // To store referral links
    public Map<String, List<String>> pendingRewards; // Store pending rewards for next login
    private Map<UUID, Long> playerOnlineTime; // Map to track online time before granting rewards
    private SQLiteStorage storage;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        storage = new SQLiteStorage(this);
        referralLinks = storage.loadReferralLinks();
        pendingRewards = new HashMap<>();
        playerOnlineTime = new HashMap<>();

        saveDefaultConfig();
        reloadConfig();

        // Register commands
        this.getCommand("referral-link").setExecutor(new ReferralLinkCommand(this));
        this.getCommand("check-domain").setExecutor(new CheckDomainCommand(this));
        this.getCommand("remove-referral-link").setExecutor(new RemoveReferralCommand(this));
        this.getCommand("referralcount").setExecutor(new ReferralCountCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new EventListener(this), this);

        // Register placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPI(this).register();
        }

        // Further initialization...
        validateConfigSetup();
    }

    private void validateConfigSetup() {
        String apiKey = getConfig().getString("cloudfare-api-key");
        String domain = getConfig().getString("domain");

        if (apiKey == null || apiKey.equals("<PASTE_YOUR_KEY_HERE>") || apiKey.trim().isEmpty()) {
            getLogger().severe("Cloudflare API key is not set in config.yml. Plugin will not function without it!");
        }

        if (domain == null || domain.trim().isEmpty()) {
            getLogger().warning("Server domain is not set in config.yml. The plugin may not function correctly.");
        }

    }

    public boolean createDNSRecord(String playerName) {
        String apiKey = getConfig().getString("cloudfare-api-key");
        String serverIP = getConfig().getString("server-ip");
        String serverDomain = getConfig().getString("domain");
        String zoneId = getConfig().getString("zone-id");

        String apiUrl = "https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records";
        String aRecordName = playerName.toLowerCase() + "." + serverDomain;
        debugLog("Creating A record for " + aRecordName);

        return createRecord(apiUrl, apiKey, "A", aRecordName, serverIP);
    }

    private boolean createRecord(String apiUrl, String apiKey, String type, String name, String content) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            // Construct the JSON payload
            String json = "{\"type\":\"" + type + "\",\"name\":\"" + name + "\",\"content\":\"" + content + "\",\"ttl\":120,\"proxied\":false}";

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Send the request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseString = response.body();

            debugLog("Cloudflare API Response for " + type + " record: Status Code: " + statusCode + ", Response Body: " + responseString);
            return statusCode == 200;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String checkDNSRecord(String playerName) {
        String apiKey = getConfig().getString("cloudfare-api-key");
        String zoneIdentifier = getConfig().getString("zone-id");
        String serverDomain = getConfig().getString("domain");
        String apiUrl = "https://api.cloudflare.com/client/v4/zones/" + zoneIdentifier + "/dns_records?name=" + playerName.toLowerCase() + "." + serverDomain;
        debugLog("Constructed API URL: " + apiUrl);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseString = response.body();

            if (statusCode == 200) {
                JSONObject jsonResponse = new JSONObject(responseString);
                JSONArray resultArray = jsonResponse.getJSONArray("result");
                if (resultArray.isEmpty()) {
                    return ChatColor.RED + "Offline";
                } else {
                    return ChatColor.GREEN + "Online";
                }
            } else {
                getLogger().severe("Error fetching DNS record for " + playerName + ": " + responseString);
                return ChatColor.RED + "Error fetching DNS record, status code: " + statusCode;
            }
        } catch (IOException | InterruptedException | JSONException e) {
            getLogger().severe("Exception while fetching DNS record for " + playerName + ": " + e.getMessage());
            return ChatColor.RED + "Error fetching DNS record: " + e.getMessage();
        }
    }

    private String extractRecordId(String responseString) {
        try {
            JSONObject jsonResponse = new JSONObject(responseString);
            JSONArray resultArray = jsonResponse.getJSONArray("result");

            if (!resultArray.isEmpty()) {
                // The first record is the ID
                JSONObject firstRecord = resultArray.getJSONObject(0);
                return firstRecord.getString("id");
            } else {
                getLogger().warning("No DNS records found in the response.");
                return null;
            }
        } catch (JSONException e) {
            getLogger().severe("Error parsing JSON response: " + e.getMessage());
            return null;
        }
    }

    public boolean isReferralDomain(String domain) {
        return referralLinks.containsValue(domain);
    }

    public boolean deleteDNSRecord(String playerName) {
        String apiKey = getConfig().getString("cloudfare-api-key");
        String zoneId = getConfig().getString("zone-id");
        String serverDomain = getConfig().getString("domain");
        String apiUrl = "https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records";

        HttpClient httpClient = HttpClient.newHttpClient();

        // Fetch the DNS record ID
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?name=" + playerName.toLowerCase() + "." + serverDomain))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

            if (getResponse.statusCode() != 200) {
                getLogger().severe("Failed to fetch DNS record for " + playerName);
                return false;
            }

            // Extract the DNS record ID
            String dnsRecordId = extractRecordId(getResponse.body());
            if (dnsRecordId == null || dnsRecordId.isEmpty()) {
                getLogger().severe("Failed to extract DNS record ID for " + playerName);
                return false;
            }

            // Delete the DNS record
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/" + dnsRecordId))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .build();

            HttpResponse<String> deleteResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

            if (deleteResponse.statusCode() == 200) {
                getLogger().info("Successfully deleted DNS record for " + playerName);
                return true;
            } else {
                getLogger().severe("Failed to delete DNS record for " + playerName + ": " + deleteResponse.body());
                return false;
            }
        } catch (IOException | InterruptedException e) {
            getLogger().severe("IOException while deleting DNS record for " + playerName + ": " + e.getMessage());
            return false;
        }
    }

    public void handleReferral(final String playerName, String domain) {
        referralLinks.entrySet().stream()
                .filter(entry -> domain.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(referrer -> {
                    if (referrer.equalsIgnoreCase(playerName)) {
                        getLogger().info(playerName + " joined using their own referral link. No rewards given.");
                        return;
                    }

                    getLogger().info(playerName + " was referred by " + referrer);

                    // Get UUID of the new player
                    UUID newPlayerUUID = getServer().getOfflinePlayer(playerName).getUniqueId();

                    // Check for potential referral abuse
                    if (isAbusingReferralSystem(newPlayerUUID, referrer)) {
                        getLogger().warning("Potential referral abuse detected for " + playerName + " referred by " + referrer);
                        return;
                    }

                    // Increment referral count for referrer
                    storage.incrementReferralCount(referrer);
                    debugLog("Incremented referral count by 1");

                    // Check if the referrer is online
                    if (getServer().getPlayer(referrer) == null) {
                        // Referrer is offline, add rewards to pending
                        debugLog(referrer + " is offline, rewards will be granted next time they log in.");
                        getConfig().getStringList("referrer-rewards").forEach(command -> {
                            String cmd = command.replace("{player}", referrer);
                            addPendingReward(referrer, cmd); // Store commands to execute later
                        });
                    }

                    // Schedule to check if new player has met the required online time
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            debugLog("Running scheduled check for " + playerName);
                            if (hasMetOnlineRequirement(newPlayerUUID)) {
                                debugLog(playerName + " met online requirement, executing rewards.");
                                executeRewards(playerName, getConfig().getStringList("new-player-rewards"), getConfig().getBoolean("new-player-random-reward"));
                            } else {
                                debugLog(playerName + " did not meet online requirement.");
                            }
                        }
                    }.runTaskLater(this, 20L * 60 * getConfig().getInt("required_online_minutes"));
                });
    }

    public boolean isAbusingReferralSystem(UUID newPlayerUUID, String referrerName) {
        String newPlayerIP = storage.getPlayerIP(newPlayerUUID);
        if (newPlayerIP == null) {
            return false; // Can't determine IP, proceed with caution
        }

        // Allow multiple referrals from the same IP if allowed
        if (getConfig().getBoolean("allow_same_ip_referrals")) {
            return false;
        }

        // Check if the IP matches any other referrers' IPs
        return storage.hasMatchingIPs(newPlayerIP, referrerName);
    }

    public void addPendingReward(String playerName, String command) {
        debugLog("Adding pending reward for " + playerName + ": " + command);
        pendingRewards.computeIfAbsent(playerName, k -> new ArrayList<>()).add(command);
        getStorage().savePendingReward(playerName, command); // Save to database
    }

    public void grantPendingRewards(String playerName) {
        List<String> rewards = getStorage().loadPendingRewards(playerName);
        debugLog("Attempting to grant " + rewards.size() + " pending rewards for " + playerName);

        if (!rewards.isEmpty()) {
            rewards.forEach(command -> {
                String formattedCommand = command.replace("{player}", playerName);
                debugLog("Executing pending reward command for " + playerName + ": " + formattedCommand);
                getServer().dispatchCommand(getServer().getConsoleSender(), formattedCommand);
            });
            getStorage().clearPendingRewards(playerName); // Clear from database after granting
            pendingRewards.remove(playerName); // Clear from in-memory storage
        } else {
            debugLog("No pending rewards found for " + playerName);
        }
    }

    public void startTrackingPlayer(UUID playerId) {
        playerOnlineTime.put(playerId, System.currentTimeMillis());
    }

    public boolean hasMetOnlineRequirement(UUID playerId) {
        long requiredTime = getConfig().getLong("required_online_minutes") * 60000; // Convert to milliseconds
        long buffer = 5000;
        Long startTime = playerOnlineTime.get(playerId);
        long timeElapsed = startTime != null ? System.currentTimeMillis() - startTime : -1;

        debugLog("Time elapsed for player " + playerId + ": " + timeElapsed + "ms, Required: " + requiredTime + "ms");
        return startTime != null && timeElapsed + buffer >= requiredTime;
    }

    private void executeRewards(String player, List<String> rewards, boolean randomReward) {
        Player bukkitPlayer = getServer().getPlayer(player);
        if (bukkitPlayer == null) {
            debugLog("Player " + player + " is not online. Rewards will be granted later.");
            rewards.forEach(command -> addPendingReward(player, command));
            return;
        }
        if (rewards.isEmpty()) return;

        Random rand = new Random();
        List<String> commandsToExecute;

        if (randomReward) {
            commandsToExecute = Collections.singletonList(rewards.get(rand.nextInt(rewards.size())));
        } else {
            commandsToExecute = new ArrayList<>(rewards);
        }

        for (String command : commandsToExecute) {
            String formattedCommand = command.replace("{player}", player);
            debugLog("Executing reward command: " + formattedCommand);
            getServer().dispatchCommand(getServer().getConsoleSender(), formattedCommand);
        }
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("debug_mode")) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        storage.closeConnection();
    }

    public SQLiteStorage getStorage() { return this.storage; }
}
