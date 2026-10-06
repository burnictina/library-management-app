package hr.projekt_lab.entities;

import hr.projekt_lab.libraryutils.SelectionUtils;
import hr.projekt_lab.libraryutils.ValidationUtils;
import hr.projekt_lab.exceptions.*;

import java.time.Year;
import java.util.List;
import java.util.Optional;


public class Utils {

    public static Optional<Author> validateAuthorSelection(List<Author> authors, int index)
            throws AuthorNotFoundException {

        Optional<Author> optional = chooseFromList(authors, index);

        if (optional.isEmpty()) {
            throw new AuthorNotFoundException("Autor s tim rednim brojem ne postoji! Pokušajte ponovno.");
        }

        return optional;
    }

    public static Optional<String> validateIsbn(String isbnInput) throws InvalidISBNException {
        try {
            return ValidationUtils.validateIsbn(isbnInput);
        } catch (IllegalArgumentException e) {
            throw new InvalidISBNException(e.getMessage());
        }
    }

    public static <T> Optional<T> chooseFromList(List<? extends T> list, int index) {
        return SelectionUtils.chooseFromList(list, index);
    }

    public static Optional<Integer> validateIssueNumber(String input)
            throws IllegalArgumentException {
        return ValidationUtils.validateIssueNumber(input);
    }

    public static Optional<Year> validateYear(String input)
            throws IllegalArgumentException {
        return ValidationUtils.validateYear(input);
    }
    public static void validateMemberLoanLimit(Member member) {
        if (member.getNumberOfLoans() >= 2) {
            throw new LoanLimitExceededException(
                    "Član je dosegao maksimalan broj posudbi!"
            );
        }
    }

    public static void validateBookAvailability(Book book) {
        if (!book.isAvailable()) {
            throw new IllegalStateException(
                    "Knjiga je već posuđena ili rezervirana!"
            );
        }
    }



}
