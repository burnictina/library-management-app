package hr.projekt_lab.entities;

import java.time.Year;

/**
 * predstavlja autora knjige.
 *
 * ova klasa omogućava upravljanje osnovnim podacima o autoru
 * uključujući ime, prezime, godinu rođenja i nacionalnost
 *
 * @author Tina Burnić
 * @version 1.0
 * @since Java 25
 */



public class Author extends Person{

    private Year yearOfBirth;
    private String nationality;
    private byte[] photo;

    /**
     *
     * @param firstName
     * @param lastName
     * @param yearOfBirth
     * @param nationality
     */



    public Author(String firstName, String lastName, Year yearOfBirth, String nationality) {
        this(firstName, lastName, yearOfBirth, nationality, null);
    }

    public Author(String firstName, String lastName, Year yearOfBirth, String nationality, byte[] photo) {
        super(firstName, lastName);
        this.yearOfBirth = yearOfBirth;
        this.nationality = nationality;
        this.photo = photo;
    }

    public Author() {
        super();
    }

    public Year getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(Year yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }


}
