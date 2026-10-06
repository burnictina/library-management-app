package hr.projekt_lab.controlleri;
import hr.projekt_lab.databaseUtil.BookDao;
import hr.projekt_lab.entities.Book;
import hr.projekt_lab.entities.Genre;
import hr.projekt_lab.libraryutils.ValidationUtils;
import hr.projekt_lab.utils.ControllerUtils;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.ScreenUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class BookController {
    @FXML
    private Button ispisiButton;

    @FXML
    private TableView<Book> knjigeTablica;

    @FXML
    private TableColumn<Book, String>  naslovColumn;

    @FXML
    private TableColumn<Book, String>  autorColumn;

    @FXML
    private TableColumn<Book, Year>  godIzdaColumn;

    @FXML
    private TableColumn<Book, String> isbnColumn;

    @FXML
    private TableColumn<Book, Genre>  zanrColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> criteriaComboBox;

    @FXML
    private Button searchButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button loanForSelectedBookButton;

    @FXML
    private ImageView coverImageView;

    @FXML
    private Label selectedBookTitleLabel;

    @FXML
    private Label selectedBookAuthorLabel;

    @FXML
    private Label selectedBookAvailabilityLabel;

    @FXML
    private Label coverPlaceholderLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label loadingLabel;

    private final ObservableList<Book> books =
            FXCollections.observableArrayList();

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        
        naslovColumn.setCellValueFactory(
                new PropertyValueFactory<>("title")
        );
        naslovColumn.setText(loc.getString("book.title"));
        
        autorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getAuthor().getFirstName()
                                + " "
                                + cellData.getValue().getAuthor().getLastName()
                ));
        autorColumn.setText(loc.getString("book.author"));
        
        godIzdaColumn.setCellValueFactory(
                new PropertyValueFactory<>("yearOfPublication")
        );
        godIzdaColumn.setText(loc.getString("book.year"));

        isbnColumn.setCellValueFactory(
                new PropertyValueFactory<>("isbn")
        );
        isbnColumn.setText(loc.getString("book.isbn"));

        zanrColumn.setCellValueFactory(
                new PropertyValueFactory<>("genre")
        );
        zanrColumn.setText(loc.getString("book.genre"));

        String titleCriteria = loc.getString("book.title");
        String authorCriteria = loc.getString("book.author");
        String genreCriteria = loc.getString("book.genre");
        criteriaComboBox.getItems().addAll(titleCriteria, authorCriteria, genreCriteria);
        criteriaComboBox.getSelectionModel().selectFirst();
        criteriaComboBox.setPromptText(loc.getString("book.search_criteria"));
        
        ispisiButton.setText(loc.getString("book.show_all"));
        searchButton.setText(loc.getString("book.search"));
        editButton.setText(loc.getString("book.edit"));
        deleteButton.setText(loc.getString("book.delete"));
        loanForSelectedBookButton.setText(loc.getString("book.loan"));

        knjigeTablica.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        naslovColumn.prefWidthProperty().bind(knjigeTablica.widthProperty().multiply(0.22));
        autorColumn.prefWidthProperty().bind(knjigeTablica.widthProperty().multiply(0.20));
        godIzdaColumn.prefWidthProperty().bind(knjigeTablica.widthProperty().multiply(0.12));
        isbnColumn.prefWidthProperty().bind(knjigeTablica.widthProperty().multiply(0.27));
        zanrColumn.prefWidthProperty().bind(knjigeTablica.widthProperty().multiply(0.19));

        knjigeTablica.setItems(books);
        resetCoverPanel();
        knjigeTablica.getSelectionModel().selectedItemProperty().addListener((obs, oldBook, newBook) -> {
            if (newBook == null) {
                resetCoverPanel();
                return;
            }

            selectedBookTitleLabel.setText(newBook.getTitle());
            selectedBookAuthorLabel.setText(
                    newBook.getAuthor().getFirstName() + " " + newBook.getAuthor().getLastName()
            );
                selectedBookAvailabilityLabel.setText(formatAvailability(newBook));
            coverPlaceholderLabel.setVisible(false);
            coverPlaceholderLabel.setManaged(false);

            if (newBook.getCoverImage() == null) {
                coverImageView.setImage(null);
                return;
            }

            coverImageView.setImage(new Image(new ByteArrayInputStream(newBook.getCoverImage())));
        });

        // Pozadinsko paralelno učitavanje: Books + Authors + Loans (3 Task-a u thread poolu)
        loadingIndicator.setVisible(true);
        loadingLabel.setVisible(true);
        DataPreloadService.getInstance().preloadAll(
                loadedBooks -> {
                    books.setAll(loadedBooks);
                    DataPreloadService.getInstance().replaceCachedBooks(loadedBooks);
                },
                () -> {
                    loadingIndicator.setVisible(false);
                    loadingLabel.setVisible(false);
                }
        );
    }

    @FXML
    void ispisiButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        DataPreloadService.getInstance().runDbTask(
                BookDao::getAllBooks,
            loadedBooks -> {
                books.setAll(loadedBooks);
                DataPreloadService.getInstance().replaceCachedBooks(loadedBooks);
            },
                e -> DialogUtils.showWarning(loc.getString("dialog.error"), loc.getString("error.fetch_books"))
        );
    }

    @FXML
    void searchButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String query = searchField.getText().trim().toLowerCase();
        String criteria = criteriaComboBox.getValue();

        if (query.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.search_empty"));
            return;
        }

        if (criteria == null || criteria.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.criteria_empty"));
            return;
        }

        List<Book> filtered = books.stream()
                .filter(book -> {
                    String titleCriteria = loc.getString("book.title");
                    String authorCriteria = loc.getString("book.author");
                    String genreCriteria = loc.getString("book.genre");

                    if (criteria.equals(titleCriteria) || criteria.equals("Naslov")) {
                        return book.getTitle() != null
                                && book.getTitle().toLowerCase().contains(query);
                    }

                    if (criteria.equals(authorCriteria) || criteria.equals("Autor")) {
                        String fullName = book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName();
                        return fullName.toLowerCase().contains(query);
                    }

                    if (criteria.equals(genreCriteria) || criteria.equals("Žanr")) {
                        return book.getGenre() != null
                                && book.getGenre().toString().replace("_", " ").toLowerCase().contains(query);
                    }

                    return false;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("dialog.no_results"));
        }
        books.setAll(filtered);
    }

    @FXML
    void editButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Book selected = knjigeTablica.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        Optional<Book> updatedBookOptional = showBookEditDialog(selected, loc);
        if (updatedBookOptional.isEmpty()) {
            return;
        }

        BookDao.updateBook(selected.getIsbn(), updatedBookOptional.get());
        DataPreloadService.getInstance().runDbTask(
                BookDao::getAllBooks,
                newBooks -> {
                    books.setAll(newBooks);
                    DataPreloadService.getInstance().replaceCachedBooks(newBooks);
                    DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("success.book_updated"));
                },
                e -> DialogUtils.showError(loc.getString("dialog.error"), loc.getString("error.fetch_books"))
        );
    }

    private Optional<Book> showBookEditDialog(Book selected, LocalizationManager loc) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(loc.getString("book.edit_title"));
        dialog.setHeaderText(loc.getString("book.edit_header"));

        ButtonType saveButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        DialogUtils.applyDialogTheme(dialog);

        TextField titleField = new TextField(selected.getTitle());
        TextField yearField = new TextField(String.valueOf(selected.getYearOfPublication().getValue()));
        TextField isbnField = new TextField(selected.getIsbn());
        TextField totalCopiesField = new TextField(String.valueOf(selected.getTotalCopies()));

        ComboBox<Genre> genreComboBox = new ComboBox<>(FXCollections.observableArrayList(Genre.values()));
        genreComboBox.getSelectionModel().select(selected.getGenre());

        ImageView previewImageView = new ImageView();
        previewImageView.setFitHeight(220);
        previewImageView.setFitWidth(160);
        previewImageView.setPreserveRatio(true);

        VBox previewCard = new VBox(previewImageView);
        previewCard.setAlignment(Pos.CENTER);
        previewCard.setPadding(new Insets(10));
        previewCard.getStyleClass().add("cover-preview-card");

        byte[][] selectedCoverImage = new byte[][] {
                selected.getCoverImage() == null ? null : selected.getCoverImage().clone()
        };

        if (selectedCoverImage[0] != null) {
            previewImageView.setImage(new Image(new ByteArrayInputStream(selectedCoverImage[0])));
        }

        Button uploadCoverButton = new Button(loc.getString("book.upload_cover_button"));
        uploadCoverButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(loc.getString("book.upload_cover_title"));
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            java.io.File selectedFile = fileChooser.showOpenDialog(uploadCoverButton.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }

            Path filePath = selectedFile.toPath();
            try {
                selectedCoverImage[0] = Files.readAllBytes(filePath);
                previewImageView.setImage(new Image(new ByteArrayInputStream(selectedCoverImage[0])));
            } catch (IOException ex) {
                DialogUtils.showError(loc.getString("dialog.error"), loc.getString("book.cover_load_error"));
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        form.add(new Label(loc.getString("book.title")), 0, 0);
        form.add(titleField, 1, 0);
        form.add(new Label(loc.getString("book.year")), 0, 1);
        form.add(yearField, 1, 1);
        form.add(new Label(loc.getString("book.isbn")), 0, 2);
        form.add(isbnField, 1, 2);
        form.add(new Label("Broj primjeraka"), 0, 3);
        form.add(totalCopiesField, 1, 3);
        form.add(new Label(loc.getString("book.genre")), 0, 4);
        form.add(genreComboBox, 1, 4);
        form.add(uploadCoverButton, 1, 5);
        form.add(previewCard, 1, 6);

        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> dialogResult = dialog.showAndWait();
        if (dialogResult.isEmpty() || dialogResult.get() != saveButtonType) {
            return Optional.empty();
        }

        String newTitle = titleField.getText().trim();
        String yearText = yearField.getText().trim();
        String isbnText = isbnField.getText().trim();
        String totalCopiesText = totalCopiesField.getText().trim();
        Genre newGenre = genreComboBox.getValue();

        if (newTitle.isEmpty() || yearText.isEmpty() || isbnText.isEmpty() || totalCopiesText.isEmpty() || newGenre == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
            return Optional.empty();
        }

        Year publicationYear;
        try {
            publicationYear = ValidationUtils.validateYear(yearText).orElseThrow();
        } catch (IllegalArgumentException ex) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("book.year_numeric"));
            return Optional.empty();
        }

        String normalizedIsbn;
        try {
            normalizedIsbn = ValidationUtils.validateIsbn(isbnText).orElseThrow();
        } catch (IllegalArgumentException ex) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("book.isbn_invalid"));
            return Optional.empty();
        }

        int newTotalCopies;
        try {
            newTotalCopies = Integer.parseInt(totalCopiesText);
            if (newTotalCopies < 1) {
                throw new NumberFormatException("totalCopies must be >= 1");
            }
        } catch (NumberFormatException ex) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), "Broj primjeraka mora biti cijeli broj veći ili jednak 1.");
            return Optional.empty();
        }

        int borrowedCopies = selected.getTotalCopies() - selected.getAvailableCopies();
        if (newTotalCopies < borrowedCopies) {
            DialogUtils.showWarning(
                    loc.getString("dialog.warning"),
                    "Broj primjeraka ne može biti manji od trenutno posuđenih primjeraka (" + borrowedCopies + ")."
            );
            return Optional.empty();
        }

        int newAvailableCopies = newTotalCopies - borrowedCopies;

        Book updatedBook = new Book.BookBuilder(
                newTitle,
                selected.getAuthor(),
                publicationYear,
                normalizedIsbn,
                newGenre
        )
                .coverImage(selectedCoverImage[0])
                .totalCopies(newTotalCopies)
                .availableCopies(newAvailableCopies)
                .build();

        return Optional.of(updatedBook);
    }

    @FXML
    void deleteButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Book selected = knjigeTablica.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(loc.getString("confirm.delete_book"));
        confirm.setHeaderText(null);
        confirm.setContentText(loc.getString("confirm.delete_book") + ": " + selected.getTitle() + "?");
        DialogUtils.applyDialogTheme(confirm);

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                BookDao.deleteBook(selected.getIsbn());
                DataPreloadService.getInstance().runDbTask(
                        BookDao::getAllBooks,
                        newBooks -> {
                            books.setAll(newBooks);
                            DataPreloadService.getInstance().replaceCachedBooks(newBooks);
                            DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("success.book_deleted"));
                        },
                        e -> DialogUtils.showError(loc.getString("dialog.error"), loc.getString("error.fetch_books"))
                );
            }
        });
    }

    @FXML
    void loanForSelectedBookButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Book selected = knjigeTablica.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        ControllerUtils.setPendingLoanBookIsbn(selected.getIsbn());
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/loan-unos.fxml", "window.title.loan_input");
    }

    private void resetCoverPanel() {
        coverImageView.setImage(null);
        selectedBookTitleLabel.setText("-");
        selectedBookAuthorLabel.setText("-");
        selectedBookAvailabilityLabel.setText("Dostupno: -/-");
        coverPlaceholderLabel.setVisible(true);
        coverPlaceholderLabel.setManaged(true);
    }

    private String formatAvailability(Book book) {
        return "Dostupno: " + book.getAvailableCopies() + "/" + book.getTotalCopies();
    }
}
