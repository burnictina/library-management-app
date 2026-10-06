package hr.projekt_lab.entities;

/**
 * sučelje odgovorno za metode za posudbu iz knjižnice.
 */

public interface Borrowable {
    boolean isAvailable();
    void borrow();
    void returnItem();

    /**
     * Ukupan broj primjeraka. Zadani fallback je 1 za entitete
     * koji još rade u single-copy modu (npr. časopis).
     */
    default int getTotalCopies() {
        return 1;
    }

    /**
     * Broj trenutno dostupnih primjeraka.
     */
    default int getAvailableCopies() {
        return isAvailable() ? 1 : 0;
    }

    /**
     * Pomoćna metoda za prikaz je li barem jedan primjerak trenutačno posuđen.
     */
    default boolean isBorrowed() {
        return getAvailableCopies() < getTotalCopies();
    }
}
