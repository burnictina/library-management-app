package hr.projekt_lab.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.time.Year;

/**
 * predstavlja časopis iz knjižnice.
 *
 * osnovni podaci kojima se upravlja su
 * naslov, izdavač, broj izdanja, godina izdanja, mjesec izdanja i kategorija
 *
 * @author Tina Burnić
 * @version 1.0
 * @since Java 25
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "magazine")
@XmlAccessorType(XmlAccessType.FIELD)
public class Magazine implements Borrowable {
    private String title;
    private String publisher;
    private Integer issueNumber;
    private Year publishingYear;
    private String publishingMonth;
    private String category;
    private boolean borrowed;

    private Magazine(MagazineBuilder builder) {
        this.title = builder.title;
        this.publisher = builder.publisher;
        this.issueNumber = builder.issueNumber;
        this.publishingYear = builder.publishingYear;
        this.publishingMonth = builder.publishingMonth;
        this.category = builder.category;
        this.borrowed = builder.borrowed;
    }

    public Magazine() {}

    public Magazine(String magTitle, String publisher, int issueNumber, int publishingYear, String publishingMonth, String category, boolean available) {
        this.title = magTitle;
        this.publisher = publisher;
        this.issueNumber = issueNumber;
        this.publishingYear = Year.of(publishingYear);
        this.publishingMonth = publishingMonth;
        this.category = category;
        this.borrowed = !available;
    }

    public static class MagazineBuilder{
        private String title;
        private String publisher;
        private Integer issueNumber;
        private Year publishingYear;
        private String publishingMonth;
        private String category;


        private boolean borrowed = false;

        /**
         *
         * @param title
         * @param publisher
         * @param issueNumber
         * @param publishingYear
         * @param publishingMonth
         * @param category
         */

        public MagazineBuilder(String title, String publisher, Integer issueNumber, Year publishingYear, String publishingMonth, String category) {
            this.title = title;
            this.publisher = publisher;
            this.issueNumber = issueNumber;
            this.publishingYear = publishingYear;
            this.publishingMonth = publishingMonth;
            this.category = category;
        }

        public MagazineBuilder title(String title) {
            this.title = title;
            return this;
        }

        public MagazineBuilder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        public MagazineBuilder issueNumber(Integer issueNumber) {
            this.issueNumber = issueNumber;
            return this;
        }

        public MagazineBuilder publishingYear(Year publishingYear) {
            this.publishingYear = publishingYear;
            return this;
        }

        public MagazineBuilder publishingMonth(String publishingMonth) {
            this.publishingMonth = publishingMonth;
            return this;
        }

        public MagazineBuilder category(String category) {
            this.category = category;
            return this;
        }

        public MagazineBuilder borrowed(boolean borrowed) {
            this.borrowed = borrowed;
            return this;
        }

        public Magazine build() {
            return new Magazine(this);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getIssueNumber() {
        return issueNumber;
    }

    public Year getPublishingYear() {
        return publishingYear;
    }

    public String getPublishingMonth() {
        return publishingMonth;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public boolean isAvailable() {
        return !borrowed;
    }

    @Override
    public void borrow() {
        if(!borrowed){
            borrowed = true;
            System.out.println(title + " je upravo posudeno.");
        } else {
            System.out.println(title + " je vec posudeno.");
        }
    }

    @Override
    public void returnItem() {
        borrowed = false;
        System.out.println(title + " je vraceno");
    }
    @Override
    public String toString() {
        return getTitle();
    }
}
