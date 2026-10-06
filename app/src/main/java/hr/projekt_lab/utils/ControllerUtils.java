package hr.projekt_lab.utils;

import hr.projekt_lab.entities.*;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ControllerUtils {
    private static String pendingLoanBookIsbn;

    private ControllerUtils() {}
    /**
     * Generički učitava i sortira podatke u ObservableList.
     * Sortiranje je abecedno prema ključu koji se određuje za svaki tip.
     *
     * @param targetList ObservableList koja se popunjava
     * @param loader     metoda koja učitava listu podataka
     * @param filter     opcionalni filter, null ako se ne želi filtrirati
     * @param <T>        tip entiteta
     */

    public static <T> void loadData(ObservableList<T> targetList, java.util.function.Supplier<Optional<List<T>>> loader, Predicate<T> filter) {
        targetList.clear();

        List<T> result = loader.get()
                .orElse(List.of())
                .stream()
                .filter(filter != null ? filter : t -> true)
                .sorted((a,b) -> getSortingKey(a).compareToIgnoreCase(getSortingKey(b)))
                .collect(Collectors.toList());
        targetList.addAll(result);
    }

    public static void setPendingLoanBookIsbn(String isbn) {
        pendingLoanBookIsbn = isbn;
    }

    public static Optional<String> consumePendingLoanBookIsbn() {
        String value = pendingLoanBookIsbn;
        pendingLoanBookIsbn = null;
        return Optional.ofNullable(value);
    }

    private static <T> String getSortingKey(T obj) {
        if (obj instanceof Book book) {
            return book.getTitle();
        } else if (obj instanceof Magazine mag) {
            return mag.getTitle();
        } else if (obj instanceof Author author) {
            return author.getFirstName();
        } else if (obj instanceof Member member) {
            return member.getId() != null ? member.getId() : "";
        } else if (obj instanceof Loan loan) {
            String firstName = loan.getMember().getFirstName();
            String lastName = loan.getMember().getLastName();
            return firstName + " " + lastName;
        } else {
            return "";
        }
    }
}
