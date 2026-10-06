package hr.projekt_lab.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

public class LoggerUtil {
    public static final Logger LOGGER = Logger.getLogger(LoggerUtil.class.getName());
    private static FileHandler fileHandler;

    static {
        try {

            fileHandler = new FileHandler("log.xml", true);
            fileHandler.setFormatter(new XMLFormatter()); // XML format
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Ne mogu inicijalizirati Logger: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void logError(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }


    public static void logInfo(String message) {
        LOGGER.log(Level.INFO, message);
    }


}
