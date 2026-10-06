package hr.projekt_lab.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.Optional;

/**
 * predstavlja posudbu jednog člana knjižnice.
 *
 * osnovni podaci o posudbi kao što su
 * član, knjiga, datum posudbe i datum povratka
 *
 * @author Tina Burnić
 * @version 1.0
 * @since Java 25
 */

public class Loan {
    private Integer loanId;
    private Member member;
    private Optional<Book> book;
    private Optional<Magazine> magazine;
    private LocalDate loanDate;
    private LocalDate returnDate;
    private String status = "AKTIVNA";
    private LocalDate actualReturnDate;

    /**
     *
     * @param member
     * @param book
     * @param loanDate
     * @param returnDate
     */



    public Loan(Member member, Optional<Book> book, Optional<Magazine> magazine, LocalDate loanDate, LocalDate returnDate) {
        this.member = member;
        this.book = book;
        this.magazine = magazine;
        this.loanDate = loanDate;
        this.returnDate = returnDate;
        this.status = "AKTIVNA";
    }
    public Loan() {}

    public Integer getLoanId() {
        return loanId;
    }

    public void setLoanId(Integer loanId) {
        this.loanId = loanId;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }
    public Optional<Book> getBook() {
        return book;
    }

    public void setBook(Optional<Book> book) {
        this.book = book;
    }
    public Optional<Magazine> getMagazine() {
        return magazine;
    }

    public void setMagazine(Optional<Magazine> magazine) {
        this.magazine = magazine;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getActualReturnDate() {
        return actualReturnDate;
    }

    public void setActualReturnDate(LocalDate actualReturnDate) {
        this.actualReturnDate = actualReturnDate;
    }

    @JsonIgnore
    public boolean isActive() {
        return "AKTIVNA".equals(status);
    }
}
