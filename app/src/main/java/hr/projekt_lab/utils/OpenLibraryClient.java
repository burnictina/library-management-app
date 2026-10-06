package hr.projekt_lab.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public final class OpenLibraryClient {
    private static final String ISBN_URL_TEMPLATE = "https://openlibrary.org/isbn/%s.json";
    private static final String AUTHOR_URL_TEMPLATE = "https://openlibrary.org%s.json";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OpenLibraryClient() {
    }

    public static Optional<BookMetadata> fetchBookMetadataByIsbn(String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode bookJson = getJson(String.format(ISBN_URL_TEMPLATE, encodePathSegment(normalizedIsbn)));
            if (bookJson == null || bookJson.isMissingNode()) {
                return Optional.empty();
            }

            String title = readText(bookJson, "title");
            String publishDate = readText(bookJson, "publish_date");
            Integer publicationYear = extractYear(publishDate);
            String authorName = fetchFirstAuthorName(bookJson.path("authors"));
            String isbnFromMetadata = extractIsbn(bookJson);

            if ((title == null || title.isBlank()) && (authorName == null || authorName.isBlank())) {
                return Optional.empty();
            }

            return Optional.of(new BookMetadata(title, authorName, publicationYear, isbnFromMetadata));
        } catch (IOException | InterruptedException e) {
            LoggerUtil.logError("Greška pri dohvaćanju podataka s Open Library API-ja", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            return null;
        }

        return OBJECT_MAPPER.readTree(response.body());
    }

    private static String fetchFirstAuthorName(JsonNode authorsNode) throws IOException, InterruptedException {
        if (authorsNode == null || !authorsNode.isArray() || authorsNode.isEmpty()) {
            return null;
        }

        JsonNode firstAuthor = authorsNode.get(0);
        String authorKey = readText(firstAuthor, "key");
        if (authorKey == null || authorKey.isBlank()) {
            return null;
        }

        JsonNode authorJson = getJson(String.format(AUTHOR_URL_TEMPLATE, authorKey));
        if (authorJson == null || authorJson.isMissingNode()) {
            return null;
        }

        return readText(authorJson, "name");
    }

    private static String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.replace("-", "").trim();
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String readText(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer extractYear(String publishDate) {
        if (publishDate == null || publishDate.isBlank()) {
            return null;
        }

        for (String token : publishDate.split("[^0-9]+")) {
            if (token.length() == 4) {
                try {
                    return Integer.parseInt(token);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String extractIsbn(JsonNode bookJson) {
        // Prioritize ISBN-13, fall back to ISBN-10
        JsonNode isbn13 = bookJson.path("isbn_13");
        if (isbn13.isArray() && isbn13.size() > 0) {
            String isbn = isbn13.get(0).asText();
            if (isbn != null && !isbn.isBlank()) {
                return isbn;
            }
        }

        JsonNode isbn10 = bookJson.path("isbn_10");
        if (isbn10.isArray() && isbn10.size() > 0) {
            String isbn = isbn10.get(0).asText();
            if (isbn != null && !isbn.isBlank()) {
                return isbn;
            }
        }

        return null;
    }

    public record BookMetadata(String title, String authorName, Integer publicationYear, String isbn) {
    }
}
