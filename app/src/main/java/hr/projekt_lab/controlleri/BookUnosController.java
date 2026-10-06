package hr.projekt_lab.controlleri;

import hr.projekt_lab.databaseUtil.AuthorDao;
import hr.projekt_lab.databaseUtil.BookDao;
import hr.projekt_lab.entities.Author;
import hr.projekt_lab.entities.Book;
import hr.projekt_lab.entities.Genre;
import hr.projekt_lab.libraryutils.TextUtils;
import hr.projekt_lab.libraryutils.ValidationUtils;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.OpenLibraryClient;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookUnosController {
    private static final String ADD_AUTHOR_PLACEHOLDER_TOKEN = "__ADD_AUTHOR_PLACEHOLDER__";

    @FXML
    private Text titleText;

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<Author> authorComboBox;

    @FXML
    private TextField yearOfPublishingField;

    @FXML
    private TextField isbnField;

    @FXML
    private ComboBox<Genre> genreComboBox;

    @FXML
    private TextField totalCopiesField;

    @FXML
    private Button saveButton;

    @FXML
    private Button uploadCoverButton;

    @FXML
    private Button fetchIsbnButton;

    @FXML
    private ImageView coverImageView;

    private byte[] selectedCoverImage;
    private Author addAuthorPlaceholder;
    private Author lastSelectedAuthor;
    private String addAuthorPlaceholderText;

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        
        titleText.setText(loc.getString("book.unos_title"));
        titleField.setPromptText(loc.getString("book.unos_prompt_title"));
        authorComboBox.setPromptText(loc.getString("book.unos_prompt_author"));
        yearOfPublishingField.setPromptText(loc.getString("book.unos_prompt_year"));
        isbnField.setPromptText(loc.getString("book.unos_prompt_isbn"));
        genreComboBox.setPromptText(loc.getString("book.unos_prompt_genre"));
        totalCopiesField.setPromptText("npr. 3");
        saveButton.setText(loc.getString("book.save_button"));
        uploadCoverButton.setText(loc.getString("book.upload_cover_button"));
        fetchIsbnButton.setText(loc.getString("book.fetch_by_isbn_button"));
        addAuthorPlaceholderText = loc.getString("book.author.add_new");
        
        setupAuthorComboBox();
        refreshAuthorsAndSelect(null);
        authorComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Author author, boolean empty) {
                super.updateItem(author, empty);
                if (empty || author == null) {
                    setText(null);
                    return;
                }
                setText(isAddAuthorPlaceholder(author)
                        ? addAuthorPlaceholderText
                        : author.getFirstName() + " " + author.getLastName());
            }
        });
        authorComboBox.setButtonCell(authorComboBox.getCellFactory().call(null));

        genreComboBox.setItems(FXCollections.observableArrayList(Genre.values()));

    }

    private void setupAuthorComboBox() {
        addAuthorPlaceholder = new Author();
        addAuthorPlaceholder.setFirstName(ADD_AUTHOR_PLACEHOLDER_TOKEN);
        addAuthorPlaceholder.setLastName(ADD_AUTHOR_PLACEHOLDER_TOKEN);
    }

    private void refreshAuthorsAndSelect(Author authorToSelect) {
        List<Author> authors = new ArrayList<>(AuthorDao.getAllAuthors());
        authors.add(addAuthorPlaceholder);
        authorComboBox.setItems(FXCollections.observableArrayList(authors));

        if (authorToSelect == null) {
            return;
        }

        authorComboBox.getItems().stream()
                .filter(author -> isSameAuthor(author, authorToSelect))
                .findFirst()
                .ifPresentOrElse(author -> {
                            authorComboBox.getSelectionModel().select(author);
                            lastSelectedAuthor = author;
                        },
                        () -> {
                            authorComboBox.getSelectionModel().clearSelection();
                            lastSelectedAuthor = null;
                        });
    }

    private boolean isSameAuthor(Author first, Author second) {
        if (first == null || second == null || isAddAuthorPlaceholder(first) || isAddAuthorPlaceholder(second)) {
            return false;
        }

        return first.getFirstName().equals(second.getFirstName())
                && first.getLastName().equals(second.getLastName())
                && first.getNationality().equals(second.getNationality())
                && first.getYearOfBirth().equals(second.getYearOfBirth());
    }

    private boolean isAddAuthorPlaceholder(Author author) {
        return author != null
                && ADD_AUTHOR_PLACEHOLDER_TOKEN.equals(author.getFirstName())
                && ADD_AUTHOR_PLACEHOLDER_TOKEN.equals(author.getLastName());
    }

    @FXML
    void authorComboBoxOnAction(ActionEvent event) {
        Author selectedAuthor = authorComboBox.getSelectionModel().getSelectedItem();
        if (selectedAuthor == null) {
            return;
        }

        if (!isAddAuthorPlaceholder(selectedAuthor)) {
            lastSelectedAuthor = selectedAuthor;
            return;
        }

        LocalizationManager loc = LocalizationManager.getInstance();
        Optional<Author> newAuthorOptional = showAddAuthorDialog();
        if (newAuthorOptional.isEmpty()) {
            if (lastSelectedAuthor != null) {
                authorComboBox.getSelectionModel().select(lastSelectedAuthor);
            } else {
                authorComboBox.getSelectionModel().clearSelection();
            }
            return;
        }

        Author newAuthor = newAuthorOptional.get();
        if (!AuthorDao.insertAuthor(newAuthor)) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("book.author.add_error"));
            if (lastSelectedAuthor != null) {
                authorComboBox.getSelectionModel().select(lastSelectedAuthor);
            } else {
                authorComboBox.getSelectionModel().clearSelection();
            }
            return;
        }

        DataPreloadService.getInstance().refreshAuthorsCache();

        refreshAuthorsAndSelect(newAuthor);
        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("book.author.add_success"));
    }

    private Optional<Author> showAddAuthorDialog() {
        LocalizationManager loc = LocalizationManager.getInstance();

        Dialog<Author> dialog = new Dialog<>();
        dialog.setTitle(loc.getString("book.author.dialog.title"));
        dialog.setHeaderText(loc.getString("book.author.dialog.header"));

        ButtonType saveButtonType = new ButtonType(loc.getString("author.save_button"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        DialogUtils.applyDialogTheme(dialog);

        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField yearOfBirthField = new TextField();
        TextField nationalityField = new TextField();

        firstNameField.setPromptText(loc.getString("author.unos_prompt_first_name"));
        lastNameField.setPromptText(loc.getString("author.unos_prompt_last_name"));
        yearOfBirthField.setPromptText(loc.getString("author.unos_prompt_year_of_birth"));
        nationalityField.setPromptText(loc.getString("author.unos_prompt_nationality"));

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10.0);
        gridPane.setVgap(10.0);
        gridPane.add(new Label(loc.getString("author.first_name")), 0, 0);
        gridPane.add(firstNameField, 1, 0);
        gridPane.add(new Label(loc.getString("author.last_name")), 0, 1);
        gridPane.add(lastNameField, 1, 1);
        gridPane.add(new Label(loc.getString("author.year_of_birth")), 0, 2);
        gridPane.add(yearOfBirthField, 1, 2);
        gridPane.add(new Label(loc.getString("author.nationality")), 0, 3);
        gridPane.add(nationalityField, 1, 3);

        dialog.getDialogPane().setContent(gridPane);

        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButtonNode.addEventFilter(ActionEvent.ACTION, actionEvent -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String yearText = yearOfBirthField.getText().trim();
            String nationality = nationalityField.getText().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || yearText.isEmpty() || nationality.isEmpty()) {
                DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
                actionEvent.consume();
                return;
            }

            try {
                ValidationUtils.validateYear(yearText).orElseThrow();
            } catch (IllegalArgumentException e) {
                DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("author.year_numeric"));
                actionEvent.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            Year yearOfBirth = ValidationUtils.validateYear(yearOfBirthField.getText().trim()).orElseThrow();
            return new Author(
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    yearOfBirth,
                    nationalityField.getText().trim()
            );
        });

        return dialog.showAndWait();
    }

    @FXML
    void fetchIsbnButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String isbn = isbnField.getText() == null ? "" : isbnField.getText().trim();
        if (isbn.isBlank()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("book.isbn_required_for_fetch"));
            return;
        }

        var metadataOptional = OpenLibraryClient.fetchBookMetadataByIsbn(isbn);
        if (metadataOptional.isEmpty()) {
            DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("book.fetch_not_found"));
            return;
        }

        var metadata = metadataOptional.get();
        if (metadata.title() != null && !metadata.title().isBlank()) {
            titleField.setText(metadata.title());
        }
        if (metadata.publicationYear() != null) {
            yearOfPublishingField.setText(String.valueOf(metadata.publicationYear()));
        }
        if (metadata.isbn() != null && !metadata.isbn().isBlank()) {
            isbnField.setText(metadata.isbn());
        }
        if (metadata.authorName() != null && !metadata.authorName().isBlank()) {
            selectMatchingAuthor(metadata.authorName());
        }

        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("book.fetch_success"));
    }

    private void selectMatchingAuthor(String authorName) {
        String normalizedTarget = TextUtils.normalizeLowercase(authorName);
        authorComboBox.getItems().stream()
                .filter(author -> TextUtils.normalizeLowercase(author.getFirstName() + " " + author.getLastName()).equals(normalizedTarget))
                .findFirst()
                .ifPresent(author -> authorComboBox.getSelectionModel().select(author));
    }

    @FXML
    void uploadCoverButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(loc.getString("book.upload_cover_title"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Window window = uploadCoverButton.getScene().getWindow();
        java.io.File selectedFile = fileChooser.showOpenDialog(window);
        if (selectedFile == null) {
            return;
        }

        Path filePath = selectedFile.toPath();
        try {
            selectedCoverImage = Files.readAllBytes(filePath);
            coverImageView.setImage(new Image(new ByteArrayInputStream(selectedCoverImage)));
        } catch (IOException e) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("book.cover_load_error"));
        }
    }

    @FXML
    void saveBookButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String title = titleField.getText().trim();
        Author author = authorComboBox.getValue();
        String yearText = yearOfPublishingField.getText().trim();
        String isbn = isbnField.getText().trim();
        Genre genre = genreComboBox.getValue();
        String totalCopiesText = totalCopiesField.getText().trim();

        if (title.isEmpty() || yearText.isEmpty() || isbn.isEmpty() ||
        genre == null || author == null || isAddAuthorPlaceholder(author) || totalCopiesText.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
            return;
        }

        Year yearOfPublication;
        String normalizedIsbn;
        int totalCopies;
        try {
            yearOfPublication = ValidationUtils.validateYear(yearText).orElseThrow();
        } catch (IllegalArgumentException e) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("book.year_numeric"));
            return;
        }

        try {
            totalCopies = Integer.parseInt(totalCopiesText);
            if (totalCopies < 1) {
                throw new NumberFormatException("totalCopies must be >= 1");
            }
        } catch (NumberFormatException e) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), "Broj primjeraka mora biti cijeli broj veći ili jednak 1.");
            return;
        }

        try {
            normalizedIsbn = ValidationUtils.validateIsbn(isbn).orElseThrow();
        } catch (IllegalArgumentException e) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("book.isbn_invalid"));
            return;
        }

        Book newBook = new Book.BookBuilder(title, author, yearOfPublication, normalizedIsbn, genre)
            .coverImage(selectedCoverImage)
            .totalCopies(totalCopies)
            .availableCopies(totalCopies)
            .build();

        try {
            BookDao.insertBook(newBook);
            DataPreloadService.getInstance().refreshBooksCache();
            DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("success.book_updated"));
            clearFields();
        } catch (Exception e) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("book.add_error"));
        }
    }
    private void clearFields() {
        titleField.clear();
        authorComboBox.getSelectionModel().clearSelection();
        yearOfPublishingField.clear();
        isbnField.clear();
        genreComboBox.getSelectionModel().clearSelection();
        totalCopiesField.clear();
        selectedCoverImage = null;
        coverImageView.setImage(null);
    }
}
