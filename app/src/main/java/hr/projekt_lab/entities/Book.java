package hr.projekt_lab.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Year;

/**
 * predstavlja knjigu u knjižnici.
 *
 * ova klasa omogućava upravljanje osnovnim podacima o knjizi uključujući
 * naslov, autora, godinu izdanja, isbn, žanr te je li posuđena odnosno rezervirana
 *
 * @author Tina Burnić
 * @version 1.0
 * @since Java 25
 */


public class Book implements Borrowable {
    private String title;
    private Author author;
    private Year yearOfPublication;
    private String isbn;
    private Genre genre;
    private byte[] coverImage;
    private int totalCopies;
    private int availableCopies;

    private Book(BookBuilder builder) {
        this.title = builder.title;
        this.author = builder.author;
        this.yearOfPublication = builder.yearOfPublication;
        this.isbn = builder.isbn;
        this.genre = builder.genre;
        this.coverImage = builder.coverImage == null ? null : builder.coverImage.clone();
        this.totalCopies = builder.totalCopies;
        this.availableCopies = builder.availableCopies;
    }
    public Book() {}

    public Book(String title, Author author, Year yearOfPublication, String isbn, Genre genre) {
        this.title = title;
        this.author = author;
        this.yearOfPublication = yearOfPublication;
        this.isbn = isbn;
        this.genre = genre;
        this.coverImage = null;
        this.totalCopies = 1;
        this.availableCopies = 1;
    }


    public static class BookBuilder {
        private String title;
        private Author author;
        private Year yearOfPublication;
        private String isbn;
        private Genre genre;
        private byte[] coverImage;
        private int totalCopies = 1;
        private int availableCopies = 1;


        /**
         * konstruira novu knjigu sa zadanim podacima putem Builder patterna
         *
         * @param title
         * @param author
         * @param yearOfPublication
         * @param isbn
         * @param genre
         */

        public BookBuilder(String title, Author author, Year yearOfPublication, String isbn, Genre genre) {
            this.title = title;
            this.author = author;
            this.yearOfPublication = yearOfPublication;
            this.isbn = isbn;
            this.genre = genre;
        }

        public BookBuilder() {

        }


        public BookBuilder title(String title) {
            this.title = title;
            return this;
        }
        public BookBuilder author(Author author) {
            this.author = author;
            return this;
        }

        public BookBuilder yearOfPublication(Year yearOfPublication) {
            this.yearOfPublication = yearOfPublication;
            return this;
        }

        public BookBuilder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public BookBuilder genre(Genre genre) {
            this.genre = genre;
            return this;
        }

        public BookBuilder coverImage(byte[] coverImage) {
            this.coverImage = coverImage == null ? null : coverImage.clone();
            return this;
        }

        public BookBuilder totalCopies(int totalCopies) {
            this.totalCopies = totalCopies;
            return this;
        }

        public BookBuilder availableCopies(int availableCopies) {
            this.availableCopies = availableCopies;
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }


    public String getTitle() {
        return title;
    }

    public Author getAuthor() {
        return author;
    }

    public Year getYearOfPublication() {
        return yearOfPublication;
    }

    public String getIsbn() {
        return isbn;
    }

    public Genre getGenre() {
        return genre;
    }

    public byte[] getCoverImage() {
        return coverImage == null ? null : coverImage.clone();
    }

    public boolean isBorrowed() {
        return availableCopies < totalCopies;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    @JsonIgnore
    @Override
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    @Override
    public void borrow() {
        if (availableCopies > 0) {
            availableCopies--;
            System.out.println(title + " je upravo posudeno.");
        } else {
            System.out.println(title + " je vec posuden.");
        }
    }

    @Override
    public void returnItem() {
        if (availableCopies < totalCopies) {
            availableCopies++;
        }
        System.out.println(title + " je vraceno");
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
