package hr.projekt_lab.databaseUtil;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

public class DatabaseUtil {
    private static final String DATABASE_FILE = "data/database.properties";
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());

    static {
        try {
            FileHandler fileHandler = new FileHandler("log.xml", true);
            fileHandler.setFormatter(new XMLFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

            Class.forName("org.h2.Driver");
            LOGGER.info("Driver je uspješno učitan.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "H2 Driver nije pronađen. Provjerite dependencies.", e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Problem sa log datotekom.", e);
        }
    }

    public static  Connection connectToDatabase() throws SQLException, IOException {
        try (FileReader reader = new FileReader(DATABASE_FILE)) {
            Properties properties = new Properties();
            properties.load(reader);

            String url  = properties.getProperty("bazaPodatakaUrl");
            String user = properties.getProperty("korisnickoIme");
            String pass = properties.getProperty("lozinka");

            LOGGER.info("Povezivanje na bazu: " + url);
            return DriverManager.getConnection(url, user, pass);
        }
    }
}
