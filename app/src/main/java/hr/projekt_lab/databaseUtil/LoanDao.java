package hr.projekt_lab.databaseUtil;

import hr.projekt_lab.entities.*;
import hr.projekt_lab.entities.Book;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static hr.projekt_lab.utils.LoggerUtil.LOGGER;

public class LoanDao {
        private static void ensureStatusColumnsExist(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE loans ADD COLUMN IF NOT EXISTS status VARCHAR(10) DEFAULT 'AKTIVNA'");
                        stmt.execute("ALTER TABLE loans ADD COLUMN IF NOT EXISTS actual_return_date DATE");
                        stmt.execute("UPDATE loans SET status = 'AKTIVNA' WHERE status IS NULL");
                }
        }

    public static List<Loan> getAllLoans() {
        List<Loan> loans = new ArrayList<>();

        String sql = """
                SELECT l.id AS loan_id, l.loan_date, l.return_date,
                       l.status, l.actual_return_date,
                       m.id AS member_id, m.first_name AS member_first, m.last_name AS member_last,
                       m.member_date, m.number_of_loans,
                          b.id AS book_id, b.title AS book_title, b.isbn, b.year_of_publication, b.genre, b.total_copies, b.available_copies,
                       a.first_name AS author_first, a.last_name AS author_last, a.year_of_birth AS author_year, a.nationality AS author_nationality,
                       mg.id AS mag_id, mg.title AS mag_title, mg.publisher, mg.issue_number, mg.publishing_year, mg.publishing_month, mg.category, mg.available
                FROM loans l
                JOIN members m ON l.member_id = m.id
                LEFT JOIN books b ON l.book_id = b.id
                LEFT JOIN authors a ON b.author_id = a.id
                LEFT JOIN magazines mg ON l.magazine_id = mg.id
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase();
             ) {
            ensureStatusColumnsExist(conn);
            try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                Member member = new Member(
                        rs.getString("member_first"),
                        rs.getString("member_last"),
                        rs.getString("member_id"),
                        rs.getDate("member_date").toLocalDate(),
                        rs.getInt("number_of_loans")
                );

                Optional<Book> book = Optional.empty();
                if (rs.getString("book_title") != null) {
                    Author author = new Author(
                            rs.getString("author_first"),
                            rs.getString("author_last"),
                            java.time.Year.of(rs.getInt("author_year")),
                            rs.getString("author_nationality")
                    );
                    Book b = new Book.BookBuilder(
                            rs.getString("book_title"),
                            author,
                            Year.of(rs.getInt("year_of_publication")),
                            rs.getString("isbn"),
                            Genre.valueOf(rs.getString("genre"))
                    )
                            .totalCopies(rs.getInt("total_copies"))
                            .availableCopies(rs.getInt("available_copies"))
                            .build();

                    book = Optional.of(b);
                }

                Optional<Magazine> magazine = Optional.empty();
                if (rs.getString("mag_title") != null) {
                    Magazine mg = new Magazine(
                            rs.getString("mag_title"),
                            rs.getString("publisher"),
                            rs.getInt("issue_number"),
                            rs.getInt("publishing_year"),
                            rs.getString("publishing_month"),
                            rs.getString("category"),
                            rs.getBoolean("available")
                    );
                    magazine = Optional.of(mg);
                }
                Loan loan = new Loan(
                        member,
                        book,
                        magazine,
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("return_date").toLocalDate()
                );
                loan.setLoanId(rs.getInt("loan_id"));
                                String status = rs.getString("status");
                                loan.setStatus(status != null ? status : "AKTIVNA");
                                Date actualReturnDateSql = rs.getDate("actual_return_date");
                                if (actualReturnDateSql != null) {
                                        loan.setActualReturnDate(actualReturnDateSql.toLocalDate());
                                }

                loans.add(loan);
            }

                        }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom dohvaćanja posudbi iz baze", e);
        }

        return loans;
    }

        public static int countLoansByMemberId(String memberId) {
                if (memberId == null || memberId.isBlank()) {
                        return 0;
                }

                String sql = "SELECT COUNT(*) AS loan_count FROM loans WHERE member_id = ? AND status = 'AKTIVNA'";

                try (Connection conn = DatabaseUtil.connectToDatabase();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {
                        ensureStatusColumnsExist(conn);

                        stmt.setString(1, memberId);

                        try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                        return rs.getInt("loan_count");
                                }
                        }
                } catch (SQLException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Greška prilikom brojanja posudbi člana", e);
                }

                return 0;
        }

        public static boolean insertLoan(Loan loan) {

                String sql = """
                                INSERT INTO loans (member_id, book_id, magazine_id, loan_date, return_date)
                                VALUES (?, ?, ?, ?, ?)
                                """;

                try (Connection conn = DatabaseUtil.connectToDatabase();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setString(1, loan.getMember().getId());

                        if (loan.getBook().isPresent()) {
                                stmt.setInt(2, getBookIdByIsbn(conn, loan.getBook().get().getIsbn()));
                        } else {
                                stmt.setNull(2, java.sql.Types.INTEGER);
                        }

                        if (loan.getMagazine().isPresent()) {
                                stmt.setInt(3, getMagazineIdByTitle(conn, loan.getMagazine().get().getTitle()));
                        } else {
                                stmt.setNull(3, java.sql.Types.INTEGER);
                        }

                        stmt.setDate(4, Date.valueOf(loan.getLoanDate()));
                        stmt.setDate(5, Date.valueOf(loan.getReturnDate()));

                        return stmt.executeUpdate() == 1;
                } catch (SQLException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Greška prilikom unosa posudbe u bazu", e);
                        return false;
                }
        }

        public static void updateLoan(Loan loan) {
                if (loan.getLoanId() == null) {
                        LOGGER.log(Level.WARNING, "Nije moguće ažurirati posudbu bez ID-a.");
                        return;
                }

                String sql = """
                                UPDATE loans
                                SET member_id = ?, book_id = ?, magazine_id = ?, loan_date = ?, return_date = ?
                                WHERE id = ?
                                """;

                try (Connection conn = DatabaseUtil.connectToDatabase();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setString(1, loan.getMember().getId());

                        if (loan.getBook().isPresent()) {
                                stmt.setInt(2, getBookIdByIsbn(conn, loan.getBook().get().getIsbn()));
                        } else {
                                stmt.setNull(2, java.sql.Types.INTEGER);
                        }

                        if (loan.getMagazine().isPresent()) {
                                stmt.setInt(3, getMagazineIdByTitle(conn, loan.getMagazine().get().getTitle()));
                        } else {
                                stmt.setNull(3, java.sql.Types.INTEGER);
                        }

                        stmt.setDate(4, Date.valueOf(loan.getLoanDate()));
                        stmt.setDate(5, Date.valueOf(loan.getReturnDate()));
                        stmt.setInt(6, loan.getLoanId());

                        stmt.executeUpdate();
                } catch (SQLException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Greška prilikom ažuriranja posudbe u bazi", e);
                }
        }

        public static void deleteLoan(int loanId) {
                String sql = "DELETE FROM loans WHERE id = ?";

                try (Connection conn = DatabaseUtil.connectToDatabase();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setInt(1, loanId);
                        stmt.executeUpdate();
                } catch (SQLException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Greška prilikom brisanja posudbe iz baze", e);
                }
        }

        public static void markAsReturned(int loanId) {
                String selectSql = """
                        SELECT b.isbn AS isbn, l.status AS status
                        FROM loans l
                        LEFT JOIN books b ON l.book_id = b.id
                        WHERE l.id = ?
                        """;
                String updateSql = "UPDATE loans SET status = 'VRACENA', actual_return_date = ? WHERE id = ? AND status = 'AKTIVNA'";

                try (Connection conn = DatabaseUtil.connectToDatabase()) {
                        ensureStatusColumnsExist(conn);

                        String isbnToReturn = null;
                        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                                selectStmt.setInt(1, loanId);
                                try (ResultSet rs = selectStmt.executeQuery()) {
                                        if (!rs.next()) {
                                                return;
                                        }
                                        if (!"AKTIVNA".equals(rs.getString("status"))) {
                                                return;
                                        }
                                        isbnToReturn = rs.getString("isbn");
                                }
                        }

                        int updatedRows;
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setDate(1, Date.valueOf(LocalDate.now()));
                                updateStmt.setInt(2, loanId);
                                updatedRows = updateStmt.executeUpdate();
                        }

                        if (updatedRows == 1 && isbnToReturn != null && !isbnToReturn.isBlank()) {
                                BookDao.returnBook(isbnToReturn);
                        }
                } catch (SQLException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Greška prilikom označavanja posudbe kao vraćene", e);
                }
        }

        public static int cleanupAnomalousActualReturnDates() {
                String sql = """
                        UPDATE loans
                        SET actual_return_date = return_date
                        WHERE status = 'VRACENA'
                          AND (
                            actual_return_date IS NULL
                            OR actual_return_date > DATEADD('DAY', 30, return_date)
                          )
                        """;

                try (Connection conn = DatabaseUtil.connectToDatabase()) {
                        ensureStatusColumnsExist(conn);
                        try (java.sql.Statement stmt = conn.createStatement()) {
                                return stmt.executeUpdate(sql);
                        }
                } catch (SQLException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Greška pri očistovanju anomalnih actual_return_date vrijednosti", e);
                        return 0;
                }
        }

        private static int getBookIdByIsbn(Connection conn, String isbn) throws SQLException {
                String sql = "SELECT id FROM books WHERE isbn = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, isbn);
                        try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                        return rs.getInt("id");
                                }
                        }
                }
                throw new SQLException("Knjiga nije pronađena po ISBN-u: " + isbn);
        }

        private static int getMagazineIdByTitle(Connection conn, String title) throws SQLException {
                String sql = "SELECT id FROM magazines WHERE title = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, title);
                        try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                        return rs.getInt("id");
                                }
                        }
                }
                throw new SQLException("Časopis nije pronađen po naslovu: " + title);
        }
}
