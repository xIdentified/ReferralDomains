package me.xidentified.referraldomains;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                    statement.execute("CREATE TABLE IF NOT EXISTS referral_links (player_name TEXT PRIMARY KEY, domain TEXT);");
                    statement.execute("CREATE TABLE IF NOT EXISTS first_joins (player_uuid TEXT PRIMARY KEY);");
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
