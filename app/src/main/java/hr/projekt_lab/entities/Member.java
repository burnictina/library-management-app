package hr.projekt_lab.entities;

import java.time.LocalDate;

/**
 * predstavlja člana knjižnice.
 *
 * ova klasa omogućava upravljanje osnovnim podacima člana knjižnice
 * uključujući ime, prezime, id i datum učlanjenja
 *
 * @author Tina Burnić
 * @version 1.0
 * @since Java 25
 */

public class Member extends Person{
    private String id;
    private LocalDate memberDate;
    private int numberOfLoans = 0;

    /**
     *
     * @param firstName
     * @param lastName
     * @param id
     * @param memberDate
     * @param numberOfLoans
     */
    public Member(String firstName, String lastName, String id, LocalDate memberDate, int numberOfLoans) {
        super(firstName, lastName);
        this.id = id;
        this.memberDate = memberDate;
        this.numberOfLoans = numberOfLoans;
    }

    public Member(String firstName, String lastName, LocalDate memberDate) {
        super(firstName, lastName);
        this.memberDate = memberDate;
    }

    public Member() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getMemberDate() {
        return memberDate;
    }

    public void setMemberDate(LocalDate memberDate) {
        this.memberDate = memberDate;
    }

    public int getNumberOfLoans() {
        return numberOfLoans;
    }

    public void setNumberOfLoans(int numberOfLoans) {
        this.numberOfLoans = numberOfLoans;
    }

    @Override
    public String toString() {
        return getFirstName() + " " + getLastName() + " (" + getId() + ")";
    }


}
