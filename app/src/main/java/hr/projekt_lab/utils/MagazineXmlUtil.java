package hr.projekt_lab.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import hr.projekt_lab.entities.Magazine;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.File;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MagazineXmlUtil {
    private static final File MAGAZINE_XML_FILE = new File("data/magazines.xml");
    private static final File MAGAZINE_JSON_FILE = new File("data/magazines.json");

    private MagazineXmlUtil() {
    }

    public static Optional<List<Magazine>> ucitajCasopise() {
        ensureXmlInitialized();

        try {
            JAXBContext context = JAXBContext.newInstance(MagazinesXmlWrapper.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            MagazinesXmlWrapper wrapper = (MagazinesXmlWrapper) unmarshaller.unmarshal(MAGAZINE_XML_FILE);

            if (wrapper == null || wrapper.magazines == null) {
                return Optional.of(new ArrayList<>());
            }

            List<Magazine> mapped = wrapper.magazines.stream()
                    .map(MagazineXmlUtil::mapToEntity)
                    .toList();

            return Optional.of(mapped);
        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom čitanja XML datoteke časopisa.", e);
        }
    }

    public static void spremiCasopis(List<Magazine> magazines) {
        ensureXmlInitialized();

        try {
            JAXBContext context = JAXBContext.newInstance(MagazinesXmlWrapper.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            MagazinesXmlWrapper wrapper = new MagazinesXmlWrapper();
            wrapper.magazines = magazines.stream()
                    .map(MagazineXmlUtil::mapToXml)
                    .toList();

            marshaller.marshal(wrapper, MAGAZINE_XML_FILE);
        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom spremanja XML datoteke časopisa.", e);
        }
    }

    public static void dodajCasopis(Magazine magazine) {
        List<Magazine> magazines = new ArrayList<>(ucitajCasopise().orElse(List.of()));
        magazines.add(magazine);
        spremiCasopis(magazines);
    }

    public static void izmijeniCasopis(String originalTitle, Magazine updatedMagazine) {
        List<Magazine> magazines = new ArrayList<>(ucitajCasopise().orElse(List.of()));

        for (int i = 0; i < magazines.size(); i++) {
            Magazine current = magazines.get(i);
            if (current.getTitle().equalsIgnoreCase(originalTitle)) {
                magazines.set(i, updatedMagazine);
                break;
            }
        }

        spremiCasopis(magazines);
    }

    public static void obrisiCasopis(String title) {
        List<Magazine> magazines = new ArrayList<>(ucitajCasopise().orElse(List.of()));
        magazines.removeIf(m -> m.getTitle().equalsIgnoreCase(title));
        spremiCasopis(magazines);
    }

    private static void ensureXmlInitialized() {
        if (MAGAZINE_XML_FILE.exists()) {
            return;
        }

        if (MAGAZINE_JSON_FILE.exists()) {
            List<Magazine> magazinesFromJson = JsonFileUtils.readListFromFile(
                    MAGAZINE_JSON_FILE,
                    new TypeReference<>() {
                    }
            );
            spremiCasopis(magazinesFromJson);
            return;
        }

        spremiCasopis(new ArrayList<>());
    }

    private static Magazine mapToEntity(MagazineXmlItem xmlItem) {
        Year publishingYear = Year.of(xmlItem.publishingYear);
        return new Magazine.MagazineBuilder(
                xmlItem.title,
                xmlItem.publisher,
                xmlItem.issueNumber,
                publishingYear,
                xmlItem.publishingMonth,
                xmlItem.category
        ).borrowed(xmlItem.borrowed).build();
    }

    private static MagazineXmlItem mapToXml(Magazine magazine) {
        MagazineXmlItem xmlItem = new MagazineXmlItem();
        xmlItem.title = magazine.getTitle();
        xmlItem.publisher = magazine.getPublisher();
        xmlItem.issueNumber = magazine.getIssueNumber();
        xmlItem.publishingYear = magazine.getPublishingYear().getValue();
        xmlItem.publishingMonth = magazine.getPublishingMonth();
        xmlItem.category = magazine.getCategory();
        xmlItem.borrowed = !magazine.isAvailable();
        return xmlItem;
    }

    @XmlRootElement(name = "magazines")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MagazinesXmlWrapper {
        @XmlElement(name = "magazine")
        public List<MagazineXmlItem> magazines = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MagazineXmlItem {
        public String title;
        public String publisher;
        public Integer issueNumber;
        public int publishingYear;
        public String publishingMonth;
        public String category;
        public boolean borrowed;
    }
}