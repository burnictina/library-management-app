package hr.projekt_lab.entities;

/**
 *  apstraktna klasa koja predstavlja osobu.
 *
 *  koristi samo osnovne informacije, ime i prezime osobe
 *
 *  @author Tina Burnić
 *  @version 1.0
 *  @since Java 25
 */

public abstract class Person {
    private String firstName;
    private String lastName;

    /**
     *
     * @param firstName
     * @param lastName
     */

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Person() {

    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}
