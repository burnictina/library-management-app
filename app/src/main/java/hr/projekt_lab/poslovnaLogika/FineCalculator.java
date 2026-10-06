package hr.projekt_lab.poslovnaLogika;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FineCalculator {

    private final double dailyFee;
    private final int gracePeriodDays;

    public FineCalculator(double dailyFee, int gracePeriodDays) {
        if (dailyFee < 0) {
            throw new IllegalArgumentException("dailyFee must not be negative");
        }
        if (gracePeriodDays < 0) {
            throw new IllegalArgumentException("gracePeriodDays must not be negative");
        }

        this.dailyFee = dailyFee;
        this.gracePeriodDays = gracePeriodDays;
    }

    public double getDailyFee() {
        return dailyFee;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public double calculateFine(LocalDate due, LocalDate returned) {
        if (due == null || returned == null) {
            throw new IllegalArgumentException("due and returned must not be null");
        }

        long overdueDays = ChronoUnit.DAYS.between(due, returned) - gracePeriodDays;
        if (overdueDays <= 0) {
            return 0.0;
        }

        return overdueDays * dailyFee;
    }

    public boolean isAnomalousReturnDate(LocalDate due, LocalDate returned) {
        if (due == null || returned == null) {
            return false;
        }

        return returned.isAfter(due.plusDays(30 + gracePeriodDays));
    }

    public boolean isOverdue(LocalDate due, LocalDate today) {
        if (due == null || today == null) {
            throw new IllegalArgumentException("due and today must not be null");
        }

        return today.isAfter(due.plusDays(gracePeriodDays));
    }
}
