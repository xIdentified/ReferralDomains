package me.xidentified.referraldomains;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class SQLiteStorage {
    private Connection connection;
    private JavaPlugin plugin;

    public SQLiteStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            synchronized (this) {
                if (connection != null && !connection.isClosed()) {
                    return;
                }

                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/referralStorage.db");

                // Create tables if they don't exist
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS referral_links (player_name TEXT PRIMARY KEY, domain TEXT, referral_count INTEGER DEFAULT 0);");
                    statement.execute("CREATE TABLE IF NOT EXISTS first_joins (player_uuid TEXT PRIMARY KEY);");
                    statement.execute("CREATE TABLE IF NOT EXISTS pending_rewards (player_name TEXT, command TEXT);");
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Could not initialize SQLite database: " + e.getMessage());
        }
    }

    public void saveReferralLink(String playerName, String domain) {
        try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO referral_links (player_name, domain) VALUES (?, ?)")) {
            ps.setString(1, playerName);
            ps.setString(2, domain);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save referral link: " + e.getMessage());
        }
    }

    public Map<String, String> loadReferralLinks() {
        Map<String, String> links = new HashMap<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM referral_links;");
            while (rs.next()) {
                links.put(rs.getString("player_name"), rs.getString("domain"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load referral links: " + e.getMessage());
        }
        return links;
    }

    public void markPlayerJoined(UUID playerUUID) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO first_joins (player_uuid) VALUES (?)")) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not mark player as joined: " + e.getMessage());
        }
    }

    public boolean hasPlayerJoinedBefore(UUID playerUUID) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM first_joins WHERE player_uuid = ?")) {
            ps.setString(1, playerUUID.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not check player join status: " + e.getMessage());
            return false;
        }
    }

    public boolean hasReferralLink(String playerName) {
        String query = "SELECT 1 FROM referral_links WHERE player_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // True if there's at least one row - link exists
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not check referral link for " + playerName + ": " + e.getMessage());
            return false;
        }
    }

    public String getReferralLink(String playerName) {
        String query = "SELECT domain FROM referral_links WHERE player_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("domain");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not retrieve referral link for " + playerName + ": " + e.getMessage());
        }
        return null; // Return null if no link found or an error occurred
    }

    public int getReferralCount(String playerName) {
        String query = "SELECT referral_count FROM referral_links WHERE player_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("referral_count");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get referral count for " + playerName + ": " + e.getMessage());
        }
        return 0;
    }

    public void incrementReferralCount(String playerName) {
        String query = "UPDATE referral_links SET referral_count = referral_count + 1 WHERE player_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not increment referral count for " + playerName + ": " + e.getMessage());
        }
    }

    public void savePendingReward(String playerName, String command) {
        String lowerCasePlayerName = playerName.toLowerCase();
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO pending_rewards (player_name, command) VALUES (?, ?)")) {
            ps.setString(1, lowerCasePlayerName);
            ps.setString(2, command);
            // int affectedRows = ps.executeUpdate();
            // plugin.getLogger().info("Saved pending reward for " + lowerCasePlayerName + ": " + command + ". Rows affected: " + affectedRows);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save pending reward for " + lowerCasePlayerName + ": " + e.getMessage());
        }
    }

    public void clearPendingRewards(String playerName) {
        String lowerCasePlayerName = playerName.toLowerCase();
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM pending_rewards WHERE player_name = ?")) {
            ps.setString(1, lowerCasePlayerName);
            int affectedRows = ps.executeUpdate();
            // plugin.getLogger().info("Cleared pending rewards for " + lowerCasePlayerName + ". Rows affected: " + affectedRows);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not clear pending rewards for " + lowerCasePlayerName + ": " + e.getMessage());
        }
    }

    public List<String> loadPendingRewards(String playerName) {
        List<String> rewards = new ArrayList<>();
        String lowerCasePlayerName = playerName.toLowerCase();
        try (PreparedStatement ps = connection.prepareStatement("SELECT command FROM pending_rewards WHERE player_name = ?")) {
            ps.setString(1, lowerCasePlayerName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String command = rs.getString("command");
                rewards.add(command);
                // plugin.getLogger().info("Loaded pending reward for " + lowerCasePlayerName + ": " + command);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load pending rewards for " + lowerCasePlayerName + ": " + e.getMessage());
        }
        plugin.getLogger().info("Total pending rewards loaded for " + lowerCasePlayerName + ": " + rewards.size());
        return rewards;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not close SQLite connection: " + e.getMessage());
            }
        }
    }
}
