module org.example.projekt_lab {
    requires org.example.libraryutils;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires transitive javafx.graphics;
    requires com.fasterxml.jackson.core;
    requires java.logging;
    requires java.net.http;
    requires java.sql;
    requires java.prefs;
    requires jakarta.xml.bind;
    requires org.glassfish.jaxb.runtime;
    requires org.apache.pdfbox;


    opens hr.projekt_lab to javafx.fxml;

    exports hr.projekt_lab;

    exports hr.projekt_lab.controlleri;

    opens hr.projekt_lab.controlleri to javafx.fxml;

    exports hr.projekt_lab.application;

    opens hr.projekt_lab.application to javafx.fxml;

    opens hr.projekt_lab.entities to javafx.base, com.fasterxml.jackson.databind, jakarta.xml.bind;
    opens hr.projekt_lab.utils to com.fasterxml.jackson.databind, jakarta.xml.bind;
    opens hr.projekt_lab.databaseUtil to com.fasterxml.jackson.databind;
    opens hr.projekt_lab.poslovnaLogika;
}