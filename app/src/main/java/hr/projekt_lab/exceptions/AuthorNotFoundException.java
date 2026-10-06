package hr.projekt_lab.exceptions;

/**
 * iznimka krivog autora.
 * ako je unesen krivi autor kod biranja knjige
 */

public class AuthorNotFoundException extends Exception {
    public AuthorNotFoundException() {
    }

    public AuthorNotFoundException(String message) {
        super(message);
    }

    public AuthorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorNotFoundException(Throwable cause) {
        super(cause);
    }

}
