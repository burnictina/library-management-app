package hr.projekt_lab.entities;

import java.time.LocalDate;

/**
 * predstavlja rezervaciju knjige za člana knjižnice.
 */
public class Reservation {
    private Integer id;
    private Member member;
    private Book book;
    private LocalDate reservationDate;
    private String status;

    public Reservation() {}

    public Reservation(Member member, Book book, LocalDate reservationDate, String status) {
        this.member = member;
        this.book = book;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
