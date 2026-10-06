package hr.projekt_lab.libraryutils;

import java.time.Year;
import java.util.Optional;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static Optional<String> validateIsbn(String isbnInput) {
        String clean = isbnInput == null ? "" : isbnInput.replaceAll("[-\\s]", "");
        boolean isValid = clean.matches("\\d{8,13}");
        if (!isValid) {
            throw new IllegalArgumentException("Neispravan ISBN! Mora imati od 8 do 13 znamenki.");
        }
        return Optional.of(clean);
    }

    public static Optional<Integer> validateIssueNumber(String input) {
        Optional<Integer> result = Optional.ofNullable(input)
                .filter(i -> i.matches("\\d+"))
                .map(Integer::parseInt)
                .filter(i -> i > 0);

        if (result.isEmpty()) {
            throw new IllegalArgumentException("Nevažeći broj časopisa.");
        }
        return result;
    }

    public static Optional<Year> validateYear(String input) {
        Optional<Year> result = Optional.ofNullable(input)
                .filter(i -> i.matches("\\d{1,4}"))
                .map(Integer::parseInt)
                .filter(y -> y > 0 && y <= Year.now().getValue())
                .map(Year::of);

        if (result.isEmpty()) {
            throw new IllegalArgumentException("Nevažeća godina izdavanja.");
        }
        return result;
    }
}
