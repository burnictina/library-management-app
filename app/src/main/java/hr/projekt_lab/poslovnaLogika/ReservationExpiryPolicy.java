package hr.projekt_lab.poslovnaLogika;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationExpiryPolicy {

    private final int expiryDays;
    private final int warningThresholdDays;

    public ReservationExpiryPolicy(int expiryDays, int warningThresholdDays) {
        if (expiryDays < 0) {
            throw new IllegalArgumentException("expiryDays must not be negative");
        }
        if (warningThresholdDays < 0) {
            throw new IllegalArgumentException("warningThresholdDays must not be negative");
        }

        this.expiryDays = expiryDays;
        this.warningThresholdDays = warningThresholdDays;
    }

    public int getExpiryDays() {
        return expiryDays;
    }

    public int getWarningThresholdDays() {
        return warningThresholdDays;
    }

    public boolean isExpired(LocalDate notifiedDate) {
        if (notifiedDate == null) {
            throw new IllegalArgumentException("notifiedDate must not be null");
        }

        return LocalDate.now().isAfter(notifiedDate.plusDays(expiryDays));
    }

    public int daysRemaining(LocalDate notifiedDate) {
        if (notifiedDate == null) {
            throw new IllegalArgumentException("notifiedDate must not be null");
        }

        long elapsedDays = ChronoUnit.DAYS.between(notifiedDate, LocalDate.now());
        long remainingDays = expiryDays - elapsedDays;
        return (int) Math.max(0, remainingDays);
    }

    public boolean isNearingExpiry(LocalDate notifiedDate) {
        if (notifiedDate == null) {
            throw new IllegalArgumentException("notifiedDate must not be null");
        }

        if (isExpired(notifiedDate)) {
            return false;
        }

        return daysRemaining(notifiedDate) <= warningThresholdDays;
    }
}
