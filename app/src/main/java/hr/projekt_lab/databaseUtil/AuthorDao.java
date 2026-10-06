package hr.projekt_lab.databaseUtil;

import hr.projekt_lab.entities.Author;
import hr.projekt_lab.utils.LoggerUtil;

import java.io.IOException;
import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class AuthorDao {
    private static final String INSERT_AUTHOR_SQL =
            "INSERT INTO authors (first_name, last_name, year_of_birth, nationality, photo) VALUES (?, ?, ?, ?, ?)";

    private static void ensurePhotoColumnExists(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE authors ADD COLUMN IF NOT EXISTS photo BLOB");
        }
    }

    public static void saveAuthor(Author author) {
        insertAuthor(author);
    }

    public static boolean insertAuthor(Author author) {
        try (Connection conn = DatabaseUtil.connectToDatabase()) {

            ensurePhotoColumnExists(conn);

            try (PreparedStatement ps = conn.prepareStatement(INSERT_AUTHOR_SQL)) {

                ps.setString(1, author.getFirstName());
                ps.setString(2, author.getLastName());
                ps.setInt(3, author.getYearOfBirth().getValue());
                ps.setString(4, author.getNationality());
                ps.setBytes(5, author.getPhoto());

                return ps.executeUpdate() > 0;
            }

        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom spremanja autora u bazu", e);
            return false;
        }
    }

    public static List<Author> getAllAuthors() {
        List<Author> authors = new ArrayList<>();

        String sql = "SELECT * FROM authors";

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensurePhotoColumnExists(conn);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    int yearOfBirth = rs.getInt("year_of_birth");
                    String nationality = rs.getString("nationality");
                    byte[] photo = rs.getBytes("photo");

                    authors.add(new Author(firstName, lastName, Year.of(yearOfBirth), nationality, photo));
                }
            }

        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom dohvaćanja autora iz baze", e);
        }

        return authors;
    }

    public static List<Author> searchByNationality(String query) {
        List<Author> authors = new ArrayList<>();

        String sql = "SELECT * FROM authors WHERE LOWER(nationality) LIKE ?";

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensurePhotoColumnExists(conn);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, "%" + query.toLowerCase() + "%");

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String firstName = rs.getString("first_name");
                        String lastName = rs.getString("last_name");
                        int yearOfBirth = rs.getInt("year_of_birth");
                        String nationality = rs.getString("nationality");
                        byte[] photo = rs.getBytes("photo");

                        authors.add(new Author(firstName, lastName, Year.of(yearOfBirth), nationality, photo));
                    }
                }
            }

        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom pretrage autora po nacionalnosti", e);
        }

        return authors;
    }

    public static int getAuthorId(Author author) throws SQLException, IOException {
        String sql = "SELECT id FROM authors WHERE first_name = ? AND last_name = ?";
        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ensurePhotoColumnExists(conn);

            stmt.setString(1, author.getFirstName());
            stmt.setString(2, author.getLastName());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Autor nije pronađen u bazi: " + author.getFirstName() + " " + author.getLastName());
                }
            }
        }
    }

    public static void updateAuthor(Author oldAuthor, Author updatedAuthor) {
        String sql = """
                UPDATE authors
                SET first_name = ?, last_name = ?, year_of_birth = ?, nationality = ?, photo = ?
                WHERE first_name = ? AND last_name = ?
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase()) {

            ensurePhotoColumnExists(conn);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, updatedAuthor.getFirstName());
                ps.setString(2, updatedAuthor.getLastName());
                ps.setInt(3, updatedAuthor.getYearOfBirth().getValue());
                ps.setString(4, updatedAuthor.getNationality());
                ps.setBytes(5, updatedAuthor.getPhoto());
                ps.setString(6, oldAuthor.getFirstName());
                ps.setString(7, oldAuthor.getLastName());

                ps.executeUpdate();
            }
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom ažuriranja autora u bazi", e);
        }
    }

    public static void deleteAuthor(Author author) {
        String sql = "DELETE FROM authors WHERE first_name = ? AND last_name = ?";

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ensurePhotoColumnExists(conn);

            ps.setString(1, author.getFirstName());
            ps.setString(2, author.getLastName());

            ps.executeUpdate();
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom brisanja autora iz baze", e);
        }
    }

}
