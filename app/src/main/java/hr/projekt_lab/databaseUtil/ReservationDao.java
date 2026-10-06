package hr.projekt_lab.databaseUtil;

import hr.projekt_lab.entities.Author;
import hr.projekt_lab.entities.Book;
import hr.projekt_lab.entities.Genre;
import hr.projekt_lab.entities.Member;
import hr.projekt_lab.entities.Reservation;
import hr.projekt_lab.poslovnaLogika.ReservationExpiryPolicy;
import hr.projekt_lab.utils.DataPreloadService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static hr.projekt_lab.utils.LoggerUtil.LOGGER;

public class ReservationDao {

    public static void ensureReservationTableExists() {
        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureReservationTableExists(conn);
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom inicijalizacije tablice reservations", e);
        }
    }

    private static void ensureReservationTableExists(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        member_id VARCHAR(50) NOT NULL,
                        book_id INT NOT NULL,
                        reservation_date DATE NOT NULL,
                        status VARCHAR(12) NOT NULL DEFAULT 'CEKA',
                        CONSTRAINT fk_reservation_member FOREIGN KEY (member_id) REFERENCES members(id),
                        CONSTRAINT fk_reservation_book FOREIGN KEY (book_id) REFERENCES books(id)
                    )
                    """);
            stmt.execute("ALTER TABLE reservations ADD COLUMN IF NOT EXISTS notified_date DATE");
        }
    }

    public static boolean insertReservation(Reservation reservation) {
        String sql = """
                INSERT INTO reservations (member_id, book_id, reservation_date, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureReservationTableExists(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reservation.getMember().getId());
                stmt.setInt(2, getBookIdByIsbn(conn, reservation.getBook().getIsbn()));
                stmt.setDate(3, Date.valueOf(reservation.getReservationDate()));
                stmt.setString(4, reservation.getStatus());

                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom unosa rezervacije", e);
            return false;
        }
    }

    public static List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
                SELECT r.id AS reservation_id, r.reservation_date, r.status,
                       m.id AS member_id, m.first_name AS member_first, m.last_name AS member_last,
                       m.member_date, m.number_of_loans,
                       b.id AS book_id, b.title AS book_title, b.isbn, b.year_of_publication, b.genre, b.total_copies, b.available_copies,
                       a.first_name AS author_first, a.last_name AS author_last, a.year_of_birth AS author_year, a.nationality AS author_nationality
                FROM reservations r
                JOIN members m ON r.member_id = m.id
                JOIN books b ON r.book_id = b.id
                JOIN authors a ON b.author_id = a.id
                ORDER BY CASE
                    WHEN r.status = 'OBAVIJESTEN' THEN 0
                    WHEN r.status = 'CEKA' THEN 1
                    WHEN r.status = 'ISTEKLO' THEN 2
                    ELSE 3
                END,
                r.reservation_date ASC,
                r.id ASC
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureReservationTableExists(conn);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom dohvaćanja rezervacija", e);
        }

        return reservations;
    }

    public static List<Reservation> getReservationsByBookId(int bookId) {
        List<Reservation> reservations = new ArrayList<>();

        String sql = """
                SELECT r.id AS reservation_id, r.reservation_date, r.status,
                       m.id AS member_id, m.first_name AS member_first, m.last_name AS member_last,
                       m.member_date, m.number_of_loans,
                       b.id AS book_id, b.title AS book_title, b.isbn, b.year_of_publication, b.genre, b.total_copies, b.available_copies,
                       a.first_name AS author_first, a.last_name AS author_last, a.year_of_birth AS author_year, a.nationality AS author_nationality
                FROM reservations r
                JOIN members m ON r.member_id = m.id
                JOIN books b ON r.book_id = b.id
                JOIN authors a ON b.author_id = a.id
                WHERE r.book_id = ?
                ORDER BY r.reservation_date ASC, r.id ASC
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureReservationTableExists(conn);
            stmt.setInt(1, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom dohvaćanja rezervacija za knjigu", e);
        }

        return reservations;
    }

    public static boolean updateReservationStatus(int reservationId, String status) {
        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureReservationTableExists(conn);
            return updateReservationStatus(conn, reservationId, status) == 1;
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom ažuriranja statusa rezervacije", e);
            return false;
        }
    }

    public static boolean expireStaleNotifiedReservationsAndPromoteNext() {
        record StaleReservation(int reservationId, int bookId, java.time.LocalDate notifiedDate) {}

        List<StaleReservation> staleReservations = new ArrayList<>();
        String staleSql = """
                SELECT id, book_id, notified_date
                FROM reservations
                WHERE status = 'OBAVIJESTEN'
                  AND notified_date IS NOT NULL
                ORDER BY notified_date ASC, id ASC
                """;

        ReservationExpiryPolicy expiryPolicy = new ReservationExpiryPolicy(3, 0);

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureReservationTableExists(conn);
            try (PreparedStatement stmt = conn.prepareStatement(staleSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDate notifiedDate = rs.getDate("notified_date") != null 
                        ? rs.getDate("notified_date").toLocalDate() 
                        : null;
                    if (notifiedDate != null && expiryPolicy.isExpired(notifiedDate)) {
                        staleReservations.add(new StaleReservation(
                            rs.getInt("id"), 
                            rs.getInt("book_id"),
                            notifiedDate
                        ));
                    }
                }
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom pronalaska isteklih OBAVIJESTEN rezervacija", e);
            return false;
        }

        boolean anyStatusChanged = false;
        for (StaleReservation stale : staleReservations) {
            synchronized (BookDao.BORROW_LOCK) {
                try (Connection conn = DatabaseUtil.connectToDatabase()) {
                    ensureReservationTableExists(conn);
                    conn.setAutoCommit(false);
                    try {
                        int expiredRows = updateReservationStatus(conn, stale.reservationId(), "ISTEKLO");
                        if (expiredRows == 1) {
                            Optional<Integer> nextPendingId = getOldestPendingReservationIdForBook(conn, stale.bookId());
                            if (nextPendingId.isPresent()) {
                                updateReservationStatus(conn, nextPendingId.get(), "OBAVIJESTEN");
                            }
                            anyStatusChanged = true;
                        }
                        conn.commit();
                    } catch (SQLException e) {
                        try {
                            conn.rollback();
                        } catch (SQLException rollbackEx) {
                            LOGGER.log(Level.SEVERE, "Rollback nije uspio tijekom isteka rezervacije", rollbackEx);
                        }
                        LOGGER.log(Level.SEVERE,
                                "Greška tijekom transakcije isteka rezervacije id=" + stale.reservationId(), e);
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException | IOException e) {
                    LOGGER.log(Level.SEVERE,
                            "Greška pri obradi isteka rezervacije id=" + stale.reservationId(), e);
                }
            }
        }

        if (anyStatusChanged) {
            DataPreloadService cache = DataPreloadService.getInstance();
            cache.invalidateReservationsCache();
            cache.refreshReservationsCache();
        }

        return anyStatusChanged;
    }

    public static boolean deleteReservation(int reservationId) {
        String sql = "DELETE FROM reservations WHERE id = ?";

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureReservationTableExists(conn);
            stmt.setInt(1, reservationId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom brisanja rezervacije", e);
            return false;
        }
    }

    public static Optional<Reservation> getOldestPendingReservationForBook(int bookId) {
        String sql = """
                SELECT r.id AS reservation_id, r.reservation_date, r.status,
                       m.id AS member_id, m.first_name AS member_first, m.last_name AS member_last,
                       m.member_date, m.number_of_loans,
                       b.id AS book_id, b.title AS book_title, b.isbn, b.year_of_publication, b.genre, b.total_copies, b.available_copies,
                       a.first_name AS author_first, a.last_name AS author_last, a.year_of_birth AS author_year, a.nationality AS author_nationality
                FROM reservations r
                JOIN members m ON r.member_id = m.id
                JOIN books b ON r.book_id = b.id
                JOIN authors a ON b.author_id = a.id
                WHERE r.book_id = ? AND r.status = 'CEKA'
                ORDER BY r.reservation_date ASC, r.id ASC
                FETCH FIRST 1 ROW ONLY
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureReservationTableExists(conn);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, bookId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapReservation(rs));
                    }
                }
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom dohvaćanja najstarije CEKA rezervacije", e);
        }

        return Optional.empty();
    }

    public static boolean hasActiveReservation(String memberId, int bookId) {
        String sql = """
                SELECT COUNT(*) AS reservation_count
                FROM reservations
                WHERE member_id = ? AND book_id = ? AND status IN ('CEKA', 'OBAVIJESTEN')
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureReservationTableExists(conn);
            stmt.setString(1, memberId);
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("reservation_count") > 0;
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom provjere aktivne rezervacije", e);
            return false;
        }
    }

    public static Optional<Integer> findActiveReservationId(String memberId, int bookId) {
        String sql = """
                SELECT id
                FROM reservations
                WHERE member_id = ? AND book_id = ? AND status IN ('CEKA', 'OBAVIJESTEN')
                ORDER BY CASE
                    WHEN status = 'OBAVIJESTEN' THEN 0
                    WHEN status = 'CEKA' THEN 1
                    WHEN status = 'ISTEKLO' THEN 2
                    ELSE 3
                END,
                reservation_date ASC,
                id ASC
                FETCH FIRST 1 ROW ONLY
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ensureReservationTableExists(conn);
            stmt.setString(1, memberId);
            stmt.setInt(2, bookId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                }
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom dohvaćanja aktivne rezervacije člana", e);
        }

        return Optional.empty();
    }

    public static Optional<Integer> findBookIdByIsbn(String isbn) {
        String sql = "SELECT id FROM books WHERE isbn = ?";

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, isbn);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                }
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Greška prilikom dohvaćanja book_id po ISBN-u", e);
        }

        return Optional.empty();
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Member member = new Member(
                rs.getString("member_first"),
                rs.getString("member_last"),
                rs.getString("member_id"),
                rs.getDate("member_date").toLocalDate(),
                rs.getInt("number_of_loans")
        );

        Author author = new Author(
                rs.getString("author_first"),
                rs.getString("author_last"),
                Year.of(rs.getInt("author_year")),
                rs.getString("author_nationality")
        );

        Book book = new Book.BookBuilder(
                rs.getString("book_title"),
                author,
                Year.of(rs.getInt("year_of_publication")),
                rs.getString("isbn"),
                Genre.valueOf(rs.getString("genre"))
        )
                .totalCopies(rs.getInt("total_copies"))
                .availableCopies(rs.getInt("available_copies"))
                .build();

        Reservation reservation = new Reservation(
                member,
                book,
                rs.getDate("reservation_date").toLocalDate(),
                rs.getString("status")
        );
        reservation.setId(rs.getInt("reservation_id"));

        return reservation;
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

    private static int updateReservationStatus(Connection conn, int reservationId, String status) throws SQLException {
        String sql = """
                UPDATE reservations
                SET status = ?,
                    notified_date = CASE
                        WHEN ? = 'OBAVIJESTEN' THEN CURRENT_DATE
                        ELSE NULL
                    END
                WHERE id = ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, status);
            stmt.setInt(3, reservationId);
            return stmt.executeUpdate();
        }
    }

    private static Optional<Integer> getOldestPendingReservationIdForBook(Connection conn, int bookId) throws SQLException {
        String sql = """
                SELECT id
                FROM reservations
                WHERE book_id = ? AND status = 'CEKA'
                ORDER BY reservation_date ASC, id ASC
                FETCH FIRST 1 ROW ONLY
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                }
            }
        }

        return Optional.empty();
    }
}
