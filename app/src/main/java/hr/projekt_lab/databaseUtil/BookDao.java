package hr.projekt_lab.databaseUtil;

import hr.projekt_lab.entities.Author;
import hr.projekt_lab.entities.Book;
import hr.projekt_lab.entities.Genre;
import hr.projekt_lab.entities.Reservation;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.LoggerUtil;

import java.io.IOException;
import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class BookDao {
    /**
     * Monitor objekt koji štiti atomsku operaciju provjere dostupnosti
     * i smanjenja broja dostupnih primjeraka.
     *
     * Koristi se {@code synchronized} blok kako bi samo jedna dretva
     * istovremeno mogla izvesti check-then-act sekvenciju:
     *   1. je li available_copies > 0?
     *   2. ako da → available_copies = available_copies - 1
     *
     * Bez zaključavanja dvije bi dretve mogle istovremeno vidjeti
     * isti slobodan primjerak i obje uspješno kreirati posudbu.
     */
    static final Object BORROW_LOCK = new Object();

    private static void ensureBooksTableColumnsExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS cover_image BLOB");
            stmt.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS total_copies INT DEFAULT 1");
            stmt.execute("ALTER TABLE books ADD COLUMN IF NOT EXISTS available_copies INT DEFAULT 1");
            stmt.execute("UPDATE books SET total_copies = 1 WHERE total_copies IS NULL OR total_copies < 1");
            stmt.execute("UPDATE books SET available_copies = total_copies WHERE available_copies IS NULL");
            stmt.execute("UPDATE books SET available_copies = 0 WHERE available_copies < 0");
            stmt.execute("UPDATE books SET available_copies = total_copies WHERE available_copies > total_copies");
        }
    }

    public static List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();

        String sql = "SELECT b.id, b.title, b.year_of_publication, b.isbn, b.genre, b.cover_image, b.total_copies, b.available_copies, "
                + "a.first_name, a.last_name, a.year_of_birth, a.nationality "
                + "FROM books b "
                + "JOIN authors a ON b.author_id = a.id";

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureBooksTableColumnsExist(conn);

            try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Author author = new Author(
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    Year.of(rs.getInt("year_of_birth")),
                    rs.getString("nationality")
                );

                Book book = new Book.BookBuilder(
                    rs.getString("title"),
                    author,
                    Year.of(rs.getInt("year_of_publication")),
                    rs.getString("isbn"),
                    Genre.valueOf(rs.getString("genre"))
                )
                    .coverImage(rs.getBytes("cover_image"))
                    .totalCopies(rs.getInt("total_copies"))
                    .availableCopies(rs.getInt("available_copies"))
                    .build();

                books.add(book);
            }
            }

        } catch (SQLException  | IOException e) {
            LoggerUtil.logError("Greška prilikom dohvaćanja knjiga iz baze", e);
        }

        return books;
    }

    public static Book getBookByIsbn(String isbn) {
        String sql = String.format("""
                 SELECT b.id, b.title, b.year_of_publication, b.isbn, b.genre, b.cover_image, b.total_copies, b.available_copies,
                       a.first_name, a.last_name, a.year_of_birth, a.nationality
                FROM books b
                JOIN authors a ON b.author_id = a.id
                WHERE b.isbn = '%s'
                """, isbn);

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureBooksTableColumnsExist(conn);

            try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                Author author = new Author(
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    Year.of(rs.getInt("year_of_birth")),
                    rs.getString("nationality")
                );

                return new Book.BookBuilder(
                    rs.getString("title"),
                    author,
                    Year.of(rs.getInt("year_of_publication")),
                    rs.getString("isbn"),
                    Genre.valueOf(rs.getString("genre"))
                )
                    .coverImage(rs.getBytes("cover_image"))
                    .totalCopies(rs.getInt("total_copies"))
                    .availableCopies(rs.getInt("available_copies"))
                    .build();
            }
            }

        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom dohvaćanja knjiga iz baze", e);
        }

        return null;
    }

    public static void insertBook(Book book) throws SQLException, IOException {
        String sql = "INSERT INTO books (title, author_id, year_of_publication, isbn, genre, cover_image, total_copies, available_copies) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ensureBooksTableColumnsExist(conn);

            int authorId = AuthorDao.getAuthorId(book.getAuthor());

            stmt.setString(1, book.getTitle());
            stmt.setInt(2, authorId);
            stmt.setInt(3, book.getYearOfPublication().getValue());
            stmt.setString(4, book.getIsbn());
            stmt.setString(5, book.getGenre().name());
            stmt.setBytes(6, book.getCoverImage());
            stmt.setInt(7, book.getTotalCopies());
            stmt.setInt(8, book.getAvailableCopies());

            stmt.executeUpdate();
        }
    }

    public static void updateBook(String oldIsbn, Book updatedBook) {
        String sql = """
                UPDATE books
            SET title = ?, author_id = ?, year_of_publication = ?, isbn = ?, genre = ?, cover_image = ?, total_copies = ?, available_copies = ?
                WHERE isbn = ?
                """;

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ensureBooksTableColumnsExist(conn);

            int authorId = AuthorDao.getAuthorId(updatedBook.getAuthor());

            stmt.setString(1, updatedBook.getTitle());
            stmt.setInt(2, authorId);
            stmt.setInt(3, updatedBook.getYearOfPublication().getValue());
            stmt.setString(4, updatedBook.getIsbn());
            stmt.setString(5, updatedBook.getGenre().name());
            stmt.setBytes(6, updatedBook.getCoverImage());
            stmt.setInt(7, updatedBook.getTotalCopies());
            stmt.setInt(8, updatedBook.getAvailableCopies());
            stmt.setString(9, oldIsbn);

            stmt.executeUpdate();
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom ažuriranja knjige u bazi", e);
        }
    }

    public static void deleteBook(String isbn) {
        String sql = "DELETE FROM books WHERE isbn = ?";

        try (Connection conn = DatabaseUtil.connectToDatabase();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, isbn);
            stmt.executeUpdate();
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška prilikom brisanja knjige iz baze", e);
        }
    }

    /**
     * Atomski pokušava posuditi knjigu zaključavanjem nad {@link #BORROW_LOCK}.
     *
     * <p>Cijeli blok provjere dostupnosti i updatea izvršava se unutar
     * {@code synchronized(BORROW_LOCK)} kako bi se spriječilo da dvije
     * dretve istovremeno vide isti broj dostupnih primjeraka i obje kreiraju
     * posudbu (race condition / TOCTOU problem).
     *
     * @param isbn ISBN knjige
     * @return {@code true} ako je posudba uspješno zabilježena;
     *         {@code false} ako nema slobodnih primjeraka ili knjiga ne postoji
     */
    public static boolean tryBorrowBook(String isbn) {
        synchronized (BORROW_LOCK) {
            String checkSql = "SELECT available_copies FROM books WHERE isbn = ?";
            String updateSql = "UPDATE books SET available_copies = available_copies - 1 WHERE isbn = ? AND available_copies > 0";

            try (Connection conn = DatabaseUtil.connectToDatabase();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

                checkStmt.setString(1, isbn);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next() || rs.getInt("available_copies") <= 0) {
                        return false; // ne postoji ili nema dostupnih primjeraka
                    }
                }

                // Knjiga ima slobodan primjerak – smanji broj dostupnih primjeraka
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, isbn);
                    return updateStmt.executeUpdate() == 1;
                }

            } catch (SQLException | IOException e) {
                LoggerUtil.logError("Greška pri posudbi knjige (synchronized blok)", e);
                throw new RuntimeException("Posudba knjige nije uspjela: " + isbn, e);
            }
        }
    }

    /**
     * Vraćanjem knjige povećava broj dostupnih primjeraka (+1),
     * najviše do ukupnog broja primjeraka.
     * Izvršava se unutar istog {@code synchronized(BORROW_LOCK)} monitora
     * radi konzistentnosti s {@link #tryBorrowBook(String)}.
     *
     * @param isbn ISBN knjige koja se vraća
     */
    public static void returnBook(String isbn) {
        synchronized (BORROW_LOCK) {
            String updateCopiesSql = """
                UPDATE books
                SET available_copies = available_copies + 1
                WHERE isbn = ? AND available_copies < total_copies
                """;
            String selectBookIdSql = "SELECT id FROM books WHERE isbn = ?";
            try (Connection conn = DatabaseUtil.connectToDatabase();
                 PreparedStatement updateStmt = conn.prepareStatement(updateCopiesSql);
                 PreparedStatement selectBookIdStmt = conn.prepareStatement(selectBookIdSql)) {
                updateStmt.setString(1, isbn);
                int updatedRows = updateStmt.executeUpdate();

                if (updatedRows == 1) {
                    selectBookIdStmt.setString(1, isbn);
                    try (ResultSet rs = selectBookIdStmt.executeQuery()) {
                        if (rs.next()) {
                            int bookId = rs.getInt("id");
                            Optional<Reservation> oldestPendingReservation =
                                ReservationDao.getOldestPendingReservationForBook(bookId);
                            oldestPendingReservation.ifPresent(reservation ->
                                {
                                    boolean updated = ReservationDao.updateReservationStatus(reservation.getId(), "OBAVIJESTEN");
                                    if (updated) {
                                        DataPreloadService cache = DataPreloadService.getInstance();
                                        cache.invalidateReservationsCache();
                                        cache.refreshReservationsCache();
                                    }
                                }
                            );
                        }
                    }
                }
            } catch (SQLException | IOException e) {
                LoggerUtil.logError("Greška pri vraćanju knjige (synchronized blok)", e);
            }
        }
    }

    public static void reconcileAvailableCopiesWithActiveLoans() {
        String impossibleSql = """
            SELECT b.isbn, b.title, b.total_copies, COALESCE(active_loans.loan_count, 0) AS loan_count
            FROM books b
            LEFT JOIN (
                SELECT l.book_id, COUNT(*) AS loan_count
                FROM loans l
                WHERE l.status = 'AKTIVNA' AND l.book_id IS NOT NULL
                GROUP BY l.book_id
            ) active_loans ON active_loans.book_id = b.id
            WHERE COALESCE(active_loans.loan_count, 0) > b.total_copies
            """;

        String recalcSql = """
            UPDATE books b
            SET available_copies = GREATEST(
                0,
                b.total_copies - (
                    SELECT COUNT(*)
                    FROM loans l
                    WHERE l.status = 'AKTIVNA' AND l.book_id = b.id
                )
            )
            """;

        try (Connection conn = DatabaseUtil.connectToDatabase()) {
            ensureBooksTableColumnsExist(conn);

            try (PreparedStatement impossibleStmt = conn.prepareStatement(impossibleSql);
                 ResultSet rs = impossibleStmt.executeQuery()) {
                while (rs.next()) {
                    LoggerUtil.LOGGER.log(
                        Level.WARNING,
                        "Nemoguće stanje za knjigu ISBN={0}, naslov={1}: aktivne posudbe={2}, total_copies={3}",
                        new Object[]{
                            rs.getString("isbn"),
                            rs.getString("title"),
                            rs.getInt("loan_count"),
                            rs.getInt("total_copies")
                        }
                    );
                }
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(recalcSql);
            }
        } catch (SQLException | IOException e) {
            LoggerUtil.logError("Greška pri usklađivanju available_copies s aktivnim posudbama", e);
        }
    }
    }
