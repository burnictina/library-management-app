package hr.projekt_lab.utils;

import hr.projekt_lab.databaseUtil.AuthorDao;
import hr.projekt_lab.databaseUtil.BookDao;
import hr.projekt_lab.databaseUtil.LoanDao;
import hr.projekt_lab.databaseUtil.ReservationDao;
import hr.projekt_lab.entities.Author;
import hr.projekt_lab.entities.Book;
import hr.projekt_lab.entities.Loan;
import hr.projekt_lab.entities.Reservation;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Servis za paralelno pozadinsko učitavanje podataka iz baze.
 *
 * Koristi fiksan thread pool veličine 4 ({@link ExecutorService}) kako bi
 * istovremeno pokrenuo četiri {@link Task}-a: Books, Authors, Loans i Reservations.
 * Svaki Task izvršava DB upit na pozadinskoj dretvi i rezultat vraća na
 * JavaFX Application Thread putem Task.setOnSucceeded mehanizma.
 *
 * Dohvaćeni podaci se cachiraju u singletonu i dostupni su ostalim
 * kontrolerima bez ponovnog upita na bazu.
 */
public final class DataPreloadService {

    /** Thread pool s 4 dretve – jedna po zadatku (Books, Authors, Loans, Reservations). */
    private static final ExecutorService THREAD_POOL =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "db-preload-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });

    private volatile List<Book>   cachedBooks   = Collections.emptyList();
    private volatile List<Author> cachedAuthors = Collections.emptyList();
    private volatile List<Loan>   cachedLoans   = Collections.emptyList();
    private volatile List<Reservation> cachedReservations = Collections.emptyList();

    private static volatile DataPreloadService instance;

    private DataPreloadService() {}

    public static DataPreloadService getInstance() {
        if (instance == null) {
            synchronized (DataPreloadService.class) {
                if (instance == null) {
                    instance = new DataPreloadService();
                }
            }
        }
        return instance;
    }

    /**
    * Pokretanje 4 Task-a paralelno u thread poolu.
     *
     * <ul>
     *   <li><b>Books Task</b>   – poziva {@link BookDao#getAllBooks()}</li>
     *   <li><b>Authors Task</b> – poziva {@link AuthorDao#getAllAuthors()}</li>
     *   <li><b>Loans Task</b>   – poziva {@link LoanDao#getAllLoans()}</li>
    *   <li><b>Reservations Task</b> – poziva {@link ReservationDao#getAllReservations()}</li>
     * </ul>
     *
     * @param onBooksLoaded  callback pozvan na FX threadu kada Books Task završi;
     *                       prima listu knjiga
      * @param onAllComplete  callback pozvan na FX threadu kada sva 4 Task-a završe
     *                       (uspješno ili s greškom); koristi se za skrivanje indikatora
     */
    public void preloadAll(Consumer<List<Book>> onBooksLoaded, Runnable onAllComplete) {
          AtomicInteger remaining = new AtomicInteger(4);

        // ── Task 1: Books ──────────────────────────────────────────────────────
        Task<List<Book>> booksTask = new Task<>() {
            @Override
            protected List<Book> call() {
                return BookDao.getAllBooks();
            }
        };
        booksTask.setOnSucceeded(e -> {
            cachedBooks = booksTask.getValue();
            onBooksLoaded.accept(cachedBooks);
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });
        booksTask.setOnFailed(e -> {
            LoggerUtil.logError("Preload knjiga nije uspio", booksTask.getException());
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });

        // ── Task 2: Authors ────────────────────────────────────────────────────
        Task<List<Author>> authorsTask = new Task<>() {
            @Override
            protected List<Author> call() {
                return AuthorDao.getAllAuthors();
            }
        };
        authorsTask.setOnSucceeded(e -> {
            cachedAuthors = authorsTask.getValue();
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });
        authorsTask.setOnFailed(e -> {
            LoggerUtil.logError("Preload autora nije uspio", authorsTask.getException());
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });

        // ── Task 3: Loans ──────────────────────────────────────────────────────
        Task<List<Loan>> loansTask = new Task<>() {
            @Override
            protected List<Loan> call() {
                return LoanDao.getAllLoans();
            }
        };
        loansTask.setOnSucceeded(e -> {
            cachedLoans = loansTask.getValue();
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });
        loansTask.setOnFailed(e -> {
            LoggerUtil.logError("Preload posudbi nije uspio", loansTask.getException());
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });

        // ── Task 4: Reservations ───────────────────────────────────────────────
        Task<List<Reservation>> reservationsTask = new Task<>() {
            @Override
            protected List<Reservation> call() {
                return ReservationDao.getAllReservations();
            }
        };
        reservationsTask.setOnSucceeded(e -> {
            cachedReservations = reservationsTask.getValue();
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });
        reservationsTask.setOnFailed(e -> {
            LoggerUtil.logError("Preload rezervacija nije uspio", reservationsTask.getException());
            if (remaining.decrementAndGet() == 0) onAllComplete.run();
        });

        // Submitiranje sva 4 zadatka u thread pool – izvršavaju se paralelno
        THREAD_POOL.submit(booksTask);
        THREAD_POOL.submit(authorsTask);
        THREAD_POOL.submit(loansTask);
        THREAD_POOL.submit(reservationsTask);
    }

    // ── Cache getteri ──────────────────────────────────────────────────────────

    public List<Book> getCachedBooks() {
        return Collections.unmodifiableList(cachedBooks);
    }

    public List<Author> getCachedAuthors() {
        return Collections.unmodifiableList(cachedAuthors);
    }

    public List<Loan> getCachedLoans() {
        return Collections.unmodifiableList(cachedLoans);
    }

    public List<Reservation> getCachedReservations() {
        return Collections.unmodifiableList(cachedReservations);
    }

    public boolean hasCachedBooks() {
        return !cachedBooks.isEmpty();
    }

    public boolean hasCachedAuthors() {
        return !cachedAuthors.isEmpty();
    }

    public boolean hasCachedLoans() {
        return !cachedLoans.isEmpty();
    }

    public boolean hasCachedReservations() {
        return !cachedReservations.isEmpty();
    }

    /**
     * Osvježava cache posudbi iz baze.
     * Koristi se nakon unosa/izmjene/brisanja kako bi sljedeći prikaz koristio
     * aktualne podatke bez restarta aplikacije.
     */
    public void replaceCachedBooks(List<Book> books) {
        cachedBooks = List.copyOf(books);
    }

    public void replaceCachedAuthors(List<Author> authors) {
        cachedAuthors = List.copyOf(authors);
    }

    public void replaceCachedLoans(List<Loan> loans) {
        cachedLoans = List.copyOf(loans);
    }

    public void replaceCachedReservations(List<Reservation> reservations) {
        cachedReservations = List.copyOf(reservations);
    }

    public void refreshBooksCache() {
        replaceCachedBooks(BookDao.getAllBooks());
    }

    public void refreshAuthorsCache() {
        replaceCachedAuthors(AuthorDao.getAllAuthors());
    }

    public void refreshLoansCache() {
        replaceCachedLoans(LoanDao.getAllLoans());
    }

    public void refreshReservationsCache() {
        replaceCachedReservations(ReservationDao.getAllReservations());
    }

    public void invalidateLoansCache() {
        cachedLoans = Collections.emptyList();
    }

    public void invalidateReservationsCache() {
        cachedReservations = Collections.emptyList();
    }

    /**
     * Pokreće DB zadatak na pozadinskoj dretvi iz thread poola.
     * Rezultat se sigurno vraća na JavaFX Application Thread pomoću
     * {@link Platform#runLater(Runnable)} – sprječava direktan UI update
     * iz pozadinske dretve koji bi bacio {@link IllegalStateException}.
     *
     * <p>Pattern:<br>
     * {@code background thread: dbWork.get()} →
     * {@code Platform.runLater → FX thread: onFxThread.accept(result)}
     *
     * @param <T>        tip rezultata DB upita
     * @param dbWork     lambda koja se izvršava na pozadinskoj dretvi
     * @param onFxThread lambda koja prima rezultat i ažurira UI; poziva se
     *                   isključivo na FX threadu putem Platform.runLater
     * @param onError    lambda pozvana na FX threadu u slučaju greške
     */
    public <T> void runDbTask(Supplier<T> dbWork,
                               Consumer<T> onFxThread,
                               Consumer<Throwable> onError) {
        THREAD_POOL.submit(() -> {
            try {
                T result = dbWork.get();
                // Platform.runLater garantira da se TableView / ObservableList
                // ažurira samo na JavaFX Application Threadu
                Platform.runLater(() -> onFxThread.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    /**
     * Zaustavlja thread pool – poziva se pri gašenju aplikacije.
     */
    public static void shutdown() {
        THREAD_POOL.shutdown();
        try {
            if (!THREAD_POOL.awaitTermination(2, TimeUnit.SECONDS)) {
                THREAD_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            THREAD_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
