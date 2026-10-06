package hr.projekt_lab.libraryutils;

public final class TextUtils {
    private TextUtils() {
    }

    public static String normalizeWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeLowercase(String value) {
        return normalizeWhitespace(value).toLowerCase();
    }
}
