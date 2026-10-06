package hr.projekt_lab.exceptions;

/**
 * iznimka
 * ako je unesen pogrešan ISBN.
 * ISBn mora biti samo brojevi sa jednom oznakom -
 * te mora biti duljine 13
 * npr. 978-0142437223
 */

public class InvalidISBNException extends Exception {
    public InvalidISBNException() {
    }

    public InvalidISBNException(String message) {
        super(message);
    }

    public InvalidISBNException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidISBNException(Throwable cause) {
        super(cause);
    }

}
