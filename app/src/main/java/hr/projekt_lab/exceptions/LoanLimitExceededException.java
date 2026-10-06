package hr.projekt_lab.exceptions;

/**
 * iznimka.
 *
 * ako je član prekoračio mogući broj posudbi nije moguće
 * dalje izvesti posudbu
 */

public class LoanLimitExceededException extends RuntimeException {
    public LoanLimitExceededException() {
    }

    public LoanLimitExceededException(String message) {
        super(message);
    }

    public LoanLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoanLimitExceededException(Throwable cause) {
        super(cause);
    }
}
