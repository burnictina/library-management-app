package hr.projekt_lab.utils;

import hr.projekt_lab.controlleri.ReservationController;
import hr.projekt_lab.databaseUtil.LoanDao;
import hr.projekt_lab.databaseUtil.ReservationDao;
import hr.projekt_lab.entities.Loan;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static hr.projekt_lab.utils.LoggerUtil.LOGGER;

public final class BackgroundSchedulerService {

    private static final BackgroundSchedulerService INSTANCE = new BackgroundSchedulerService();

    private static final long RESERVATION_INTERVAL_MINUTES = 2L;
    private static final long LOAN_BACKUP_INTERVAL_MINUTES = 5L;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        private int threadIndex = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "background-scheduler-" + threadIndex++);
            thread.setDaemon(true);
            return thread;
        }
    });

    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile ScheduledFuture<?> reservationTask;
    private volatile ScheduledFuture<?> loanBackupTask;

    private BackgroundSchedulerService() {
    }

    public static BackgroundSchedulerService getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        reservationTask = executorService.scheduleAtFixedRate(
                this::runReservationSweep,
                RESERVATION_INTERVAL_MINUTES,
                RESERVATION_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );

        loanBackupTask = executorService.scheduleAtFixedRate(
                this::runLoanBackup,
                LOAN_BACKUP_INTERVAL_MINUTES,
                LOAN_BACKUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private void runReservationSweep() {
        try {
            boolean statusChanged = ReservationDao.expireStaleNotifiedReservationsAndPromoteNext();
            if (statusChanged) {
                Platform.runLater(ReservationController::refreshIfCurrentlyDisplayed);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Greška u pozadinskoj provjeri rezervacija", e);
        }
    }

    private void runLoanBackup() {
        try {
            List<Loan> loans = LoanDao.getAllLoans();
            LoanBinaryUtil.zapiši(loans);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Greška u pozadinskom backupu posudbi", e);
        }
    }

    public void shutdown() {
        if (reservationTask != null) {
            reservationTask.cancel(false);
        }
        if (loanBackupTask != null) {
            loanBackupTask.cancel(false);
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            started.set(false);
        }
    }
}