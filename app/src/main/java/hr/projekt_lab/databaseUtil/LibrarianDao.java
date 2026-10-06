package hr.projekt_lab.databaseUtil;

import hr.projekt_lab.entities.Librarian;
import hr.projekt_lab.utils.LoggerUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class LibrarianDao {
    private static final String DEFAULT_LIBRARIAN_USERNAME = "librarian";
    private static final String DEFAULT_LIBRARIAN_PASSWORD = "Librarian123!";
    private static final String DEFAULT_LIBRARIAN_FULL_NAME = "Library Administrator";

    private static final String PEPPER_BASE =
            System.getenv().getOrDefault("PROJEKT_LAB_PEPPER_BASE", "library-pepper-v");
    private static final int PEPPER_MIN_VERSION = 1;
    private static final int CURRENT_PEPPER_VERSION = 3;

    public static final String CREATE_LIBRARIANS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS librarians (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(100) NOT NULL UNIQUE,
                password_hash VARCHAR(64) NOT NULL,
                full_name VARCHAR(150) NOT NULL,
                registration_ts BIGINT NOT NULL
            )
            """;

    private LibrarianDao() {
    }

    public static void createTableIfNotExists() {
        try (Connection conn = DatabaseUtil.connectToDatabase();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_LIBRARIANS_TABLE_SQL);
            ensureSchemaCompatibility(conn);
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom inicijalizacije librarians tablice", e);
        }
    }

    public static void ensureDefaultAdminIfEmpty() {
        createTableIfNotExists();

        String countSql = "SELECT COUNT(*) AS cnt FROM librarians";
        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(countSql);
             ResultSet rs = stmt.executeQuery()) {

            if (!rs.next() || rs.getInt("cnt") > 0) {
                return;
            }

            boolean inserted = insertLibrarian(
                    DEFAULT_LIBRARIAN_USERNAME,
                    DEFAULT_LIBRARIAN_PASSWORD,
                    DEFAULT_LIBRARIAN_FULL_NAME
            );

            if (inserted) {
                LoggerUtil.logInfo("Kreiran je zadani administratorski račun: librarian");
            }
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška pri automatskom kreiranju zadanog administratorskog računa", e);
        }
    }

    public static boolean insertLibrarian(String username, String rawPassword, String fullName) {
        if (username == null || username.isBlank()
                || rawPassword == null || rawPassword.isBlank()
                || fullName == null || fullName.isBlank()) {
            return false;
        }

        createTableIfNotExists();

        String normalizedUsername = username.trim();
        String normalizedFullName = fullName.trim();
        long registrationTimestamp = System.currentTimeMillis();
        String salt = deriveSalt(normalizedUsername);
        String passwordHash = hashPassword(rawPassword, salt, resolvePepper(CURRENT_PEPPER_VERSION));

        String sql = """
                INSERT INTO librarians (username, password_hash, full_name, registration_ts)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizedUsername);
            stmt.setString(2, passwordHash);
            stmt.setString(3, normalizedFullName);
            stmt.setLong(4, registrationTimestamp);
            return stmt.executeUpdate() == 1;
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom unosa knjižničara u bazu", e);
            return false;
        }
    }

    public static Optional<Librarian> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        createTableIfNotExists();

        String sql = """
            SELECT id, username, password_hash, full_name, registration_ts
                FROM librarians
                WHERE username = ?
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String resolvedUsername = rs.getString("username");
                Librarian librarian = new Librarian(
                        rs.getLong("id"),
                        resolvedUsername,
                        rs.getString("password_hash"),
                        rs.getString("full_name")
                );

                return Optional.of(librarian);
            }
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom dohvaćanja knjižničara po korisničkom imenu", e);
            return Optional.empty();
        }
    }

    public static boolean verifyPassword(String rawPassword, Librarian librarian) {
        if (rawPassword == null || rawPassword.isBlank() || librarian == null) {
            return false;
        }

        String storedHash = librarian.getPasswordHash();
        String salt = deriveSalt(librarian.getUsername());
        if (storedHash == null || storedHash.isBlank() || salt.isBlank()) {
            return false;
        }

        for (int pepperVersion = PEPPER_MIN_VERSION; pepperVersion <= CURRENT_PEPPER_VERSION; pepperVersion++) {
            String pepper = resolvePepper(pepperVersion);
            String candidateHash = hashPassword(rawPassword, salt, pepper);
            if (storedHash.equals(candidateHash)) {
                return true;
            }
        }

        return false;
    }

    private static void ensureSchemaCompatibility(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE librarians ADD COLUMN IF NOT EXISTS id BIGINT AUTO_INCREMENT");
            stmt.execute("ALTER TABLE librarians ADD COLUMN IF NOT EXISTS full_name VARCHAR(150)");
            stmt.execute("ALTER TABLE librarians ADD COLUMN IF NOT EXISTS registration_ts BIGINT");

            boolean legacySaltColumnExists = columnExists(conn, "LIBRARIANS", "SALT");
            if (legacySaltColumnExists) {
                // Dev migration: old hashes used a persisted salt; clear accounts so they are recreated with the new rule-based salt.
                try {
                    stmt.execute("ALTER TABLE librarians DROP COLUMN salt");
                } catch (SQLException dropEx) {
                    LoggerUtil.logError("Nije moguće ukloniti legacy salt stupac iz librarians tablice", dropEx);
                }
                stmt.executeUpdate("DELETE FROM librarians");
                LoggerUtil.logInfo("Legacy librarians računi su resetirani zbog prelaska na rule-based salt bez pohrane u bazi.");
            }
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) {
        try (ResultSet columns = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return columns.next();
        } catch (SQLException e) {
            LoggerUtil.logError("Neuspješna provjera postojanja stupca " + tableName + "." + columnName, e);
            return false;
        }
    }

    private static String hashPassword(String rawPassword, String salt, String pepper) {
        return sha256Hex(rawPassword + ":" + salt + ":" + pepper);
    }

    private static String deriveSalt(String username) {
        if (username == null) {
            return "";
        }
        String normalizedUsername = username.trim().toLowerCase();
        String seed = normalizedUsername + ":library-salt-rule-v2";
        return sha256Hex(seed);
    }

    private static String resolvePepper(int version) {
        return PEPPER_BASE + version;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algoritam nije dostupan", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}