package hr.projekt_lab.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.prefs.Preferences;

public final class AppSettings {
    private static final String SETTINGS_FILE_PATH = "data/app-settings.properties";

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_LAST_EXPORT_DIR = "lastExportDir";
    private static final String KEY_WINDOW_WIDTH = "windowWidth";
    private static final String KEY_WINDOW_HEIGHT = "windowHeight";

    private static final String DEFAULT_LANGUAGE = "hr";
    private static final String DEFAULT_LAST_EXPORT_DIR = "data";
    private static final double DEFAULT_WINDOW_WIDTH = 1200.0;
    private static final double DEFAULT_WINDOW_HEIGHT = 760.0;

    private static final String PREF_NODE = "hr.projekt_lab";
    private static final String PREF_USERNAME = "rememberedUsername";

    private AppSettings() {
    }

    public static Properties loadSettings() {
        Properties properties = new Properties();
        File file = new File(SETTINGS_FILE_PATH);

        if (file.exists()) {
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                throw new RuntimeException("Greška prilikom učitavanja app settings datoteke.", e);
            }
        }

        ensureDefaults(properties);
        return properties;
    }

    public static void saveSettings(Properties properties) {
        ensureDefaults(properties);

        File file = new File(SETTINGS_FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, "Application settings (INI style via Properties)");
        } catch (IOException e) {
            throw new RuntimeException("Greška prilikom spremanja app settings datoteke.", e);
        }
    }

    public static String getLanguage() {
        return loadSettings().getProperty(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    public static void setLanguage(String language) {
        Properties properties = loadSettings();
        properties.setProperty(KEY_LANGUAGE, language);
        saveSettings(properties);
    }

    public static String getLastExportDir() {
        return loadSettings().getProperty(KEY_LAST_EXPORT_DIR, DEFAULT_LAST_EXPORT_DIR);
    }

    public static void setLastExportDir(String exportDir) {
        Properties properties = loadSettings();
        properties.setProperty(KEY_LAST_EXPORT_DIR, exportDir);
        saveSettings(properties);
    }

    public static double getWindowWidth() {
        String value = loadSettings().getProperty(KEY_WINDOW_WIDTH, String.valueOf(DEFAULT_WINDOW_WIDTH));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return DEFAULT_WINDOW_WIDTH;
        }
    }

    public static double getWindowHeight() {
        String value = loadSettings().getProperty(KEY_WINDOW_HEIGHT, String.valueOf(DEFAULT_WINDOW_HEIGHT));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return DEFAULT_WINDOW_HEIGHT;
        }
    }

    public static void setWindowSize(double width, double height) {
        Properties properties = loadSettings();
        properties.setProperty(KEY_WINDOW_WIDTH, String.valueOf(width));
        properties.setProperty(KEY_WINDOW_HEIGHT, String.valueOf(height));
        saveSettings(properties);
    }

    public static String getRememberedUsername() {
        Preferences preferences = Preferences.userRoot().node(PREF_NODE);
        return preferences.get(PREF_USERNAME, "");
    }

    public static void setRememberedUsername(String username) {
        Preferences preferences = Preferences.userRoot().node(PREF_NODE);
        preferences.put(PREF_USERNAME, username == null ? "" : username);
    }

    private static void ensureDefaults(Properties properties) {
        properties.putIfAbsent(KEY_LANGUAGE, DEFAULT_LANGUAGE);
        properties.putIfAbsent(KEY_LAST_EXPORT_DIR, DEFAULT_LAST_EXPORT_DIR);
        properties.putIfAbsent(KEY_WINDOW_WIDTH, String.valueOf(DEFAULT_WINDOW_WIDTH));
        properties.putIfAbsent(KEY_WINDOW_HEIGHT, String.valueOf(DEFAULT_WINDOW_HEIGHT));
    }
}
