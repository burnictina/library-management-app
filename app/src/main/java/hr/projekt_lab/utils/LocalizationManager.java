package hr.projekt_lab.utils;

import java.util.ResourceBundle;

/**
 * Manages application localization with support for multiple languages.
 * Provides centralized access to localized messages via ResourceBundle.
 * Supports language switching at runtime with persistence via AppSettings.
 *
 * @author Tina Burnić
 * @version 1.0
 * @since Java 25
 */
public class LocalizationManager {
    private static LocalizationManager instance;
    private ResourceBundle bundle;
    private String currentLanguage;

    private static final String HR = "hr";
    private static final String EN = "en";
    private static final String BUNDLE_NAME = "messages";

    private LocalizationManager() {
        currentLanguage = AppSettings.getLanguage();
        loadBundle();
    }

    /**
     * Returns singleton instance of LocalizationManager.
     */
    public static synchronized LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }
        return instance;
    }

    /**
     * Loads ResourceBundle for the current language.
     * Defaults to HR if language is not supported.
     */
    private void loadBundle() {
        try {
            if (EN.equals(currentLanguage)) {
                bundle = ResourceBundle.getBundle(BUNDLE_NAME, java.util.Locale.of(EN));
            } else {
                bundle = ResourceBundle.getBundle(BUNDLE_NAME, java.util.Locale.of(HR));
                currentLanguage = HR; // Ensure consistency
            }
        } catch (Exception e) {
            System.err.println("Error loading ResourceBundle: " + e.getMessage());
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, java.util.Locale.of(HR));
            currentLanguage = HR;
        }
    }

    /**
     * Gets localized string for the given key.
     *
     * @param key the resource key
     * @return localized string or key itself if not found
     */
    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (java.util.MissingResourceException e) {
            System.err.println("Missing translation key: " + key);
            return key;
        }
    }

    /**
     * Switches application language and reloads bundle.
     * Persists language choice to AppSettings.
     *
     * @param language "hr" for Croatian, "en" for English
     */
    public void switchLanguage(String language) {
        if ((HR.equals(language) || EN.equals(language)) && !language.equals(currentLanguage)) {
            currentLanguage = language;
            loadBundle();
            AppSettings.setLanguage(language);
        }
    }

    /**
     * Gets current language code.
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Returns active resource bundle for FXML loaders.
     */
    public ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Resets instance to reload settings (used when AppSettings changes).
     */
    public static void reset() {
        instance = null;
    }
}
