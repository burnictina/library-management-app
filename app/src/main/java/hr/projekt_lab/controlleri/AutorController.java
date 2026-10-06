package hr.projekt_lab.controlleri;

import hr.projekt_lab.databaseUtil.AuthorDao;
import hr.projekt_lab.entities.Author;
import hr.projekt_lab.libraryutils.ValidationUtils;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.LoggerUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;
import java.util.Optional;

public class AutorController {
    @FXML
    private TableView<Author> authorsTable;
    @FXML
    private TableColumn<Author, String> firstNameColumn;
    @FXML
    private TableColumn<Author, String> lastNameColumn;
    @FXML
    private TableColumn<Author, String> yearColumn;
    @FXML
    private TableColumn<Author, String> nationalityColumn;
    @FXML
    private Button showAllButton;
    @FXML
    private TextField nationalityField;
    @FXML
    private Button searchButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private ImageView authorPhotoImageView;

    @FXML
    private Label authorPhotoPlaceholderLabel;

    @FXML
    private Label selectedAuthorNameLabel;

    @FXML
    private Label selectedAuthorDetailsLabel;

    @FXML
    private Text selectedAuthorPanelTitle;

    private final ObservableList<Author> authors = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        firstNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFirstName()));
        firstNameColumn.setText(loc.getString("author.first_name"));
        
        lastNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLastName()));
        lastNameColumn.setText(loc.getString("author.last_name"));
        
        yearColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getYearOfBirth().toString()));
        yearColumn.setText(loc.getString("author.year_of_birth"));
        
        nationalityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNationality()));
        nationalityColumn.setText(loc.getString("author.nationality"));

        showAllButton.setText(loc.getString("author.show_all"));
        nationalityField.setPromptText(loc.getString("author.search_criteria"));
        searchButton.setText(loc.getString("author.search"));
        editButton.setText(loc.getString("author.edit"));
        deleteButton.setText(loc.getString("author.delete"));
        selectedAuthorPanelTitle.setText(loc.getString("author.selected_panel_title"));

        authorsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        firstNameColumn.prefWidthProperty().bind(authorsTable.widthProperty().multiply(0.27));
        lastNameColumn.prefWidthProperty().bind(authorsTable.widthProperty().multiply(0.27));
        yearColumn.prefWidthProperty().bind(authorsTable.widthProperty().multiply(0.18));
        nationalityColumn.prefWidthProperty().bind(authorsTable.widthProperty().multiply(0.28));

        DataPreloadService cache = DataPreloadService.getInstance();
        if (cache.hasCachedAuthors()) {
            authors.setAll(cache.getCachedAuthors());
        } else {
            List<Author> loadedAuthors = AuthorDao.getAllAuthors();
            authors.setAll(loadedAuthors);
            cache.replaceCachedAuthors(loadedAuthors);
        }
        authorsTable.setItems(authors);

        resetSelectedAuthorPanel();
        authorsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldAuthor, newAuthor) -> {
            if (newAuthor == null) {
                resetSelectedAuthorPanel();
                return;
            }

            selectedAuthorNameLabel.setText(newAuthor.getFirstName() + " " + newAuthor.getLastName());
            selectedAuthorDetailsLabel.setText(
                    loc.getString("author.nationality") + ": " + newAuthor.getNationality()
                            + " | "
                            + loc.getString("author.year_of_birth") + ": " + newAuthor.getYearOfBirth()
            );

            if (newAuthor.getPhoto() == null) {
                authorPhotoImageView.setImage(null);
                authorPhotoPlaceholderLabel.setText(loc.getString("author.photo_unavailable"));
                authorPhotoPlaceholderLabel.setVisible(true);
                authorPhotoPlaceholderLabel.setManaged(true);
                return;
            }

            authorPhotoPlaceholderLabel.setVisible(false);
            authorPhotoPlaceholderLabel.setManaged(false);
            authorPhotoImageView.setImage(new Image(new ByteArrayInputStream(newAuthor.getPhoto())));
        });
    }
    @FXML
    void showAllButtonOnAction(ActionEvent event) {
        DataPreloadService.getInstance().runDbTask(
                AuthorDao::getAllAuthors,
            loadedAuthors -> {
                authors.setAll(loadedAuthors);
                DataPreloadService.getInstance().replaceCachedAuthors(loadedAuthors);
            },
                e -> LoggerUtil.LOGGER.warning("Neuspješno dohvaćanje autora: " + e.getMessage())
        );
    }
    @FXML
    void searchButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String query = nationalityField.getText().trim().toLowerCase();

        if (query.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.search_empty"));
            return;
        }

        /*List<Author> filtered = AuthorJsonUtil.ucitajAutore()
                .orElse(List.of())
                .stream()
                .filter(a -> a.getNationality().toLowerCase().contains(query))
                .toList();*/
        List<Author> filtered = AuthorDao.searchByNationality(query);

        if (filtered.isEmpty()) {
            DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("error.no_authors_found"));
        }

        authors.setAll(filtered);
    }

    @FXML
    void editButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Author selected = authorsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        Optional<Author> updatedAuthorOptional = showAuthorEditDialog(selected, loc);
        if (updatedAuthorOptional.isEmpty()) {
            return;
        }

        AuthorDao.updateAuthor(selected, updatedAuthorOptional.get());
        DataPreloadService.getInstance().runDbTask(
                AuthorDao::getAllAuthors,
                newAuthors -> {
                    authors.setAll(newAuthors);
                    DataPreloadService.getInstance().replaceCachedAuthors(newAuthors);
                    DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("success.author_updated"));
                },
                e -> DialogUtils.showError(loc.getString("dialog.error"), loc.getString("error.fetch_authors"))
        );
    }

    private Optional<Author> showAuthorEditDialog(Author selected, LocalizationManager loc) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(loc.getString("author.edit_title"));
        dialog.setHeaderText(loc.getString("author.edit_header"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogUtils.applyDialogTheme(dialog);

        TextField firstNameField = new TextField(selected.getFirstName());
        TextField lastNameField = new TextField(selected.getLastName());
        TextField yearField = new TextField(String.valueOf(selected.getYearOfBirth().getValue()));
        TextField nationalityField = new TextField(selected.getNationality());
        ImageView previewImageView = new ImageView();
        previewImageView.setFitHeight(220.0);
        previewImageView.setFitWidth(160.0);
        previewImageView.setPreserveRatio(true);

        byte[][] selectedPhoto = new byte[][]{
                selected.getPhoto() == null ? null : selected.getPhoto().clone()
        };
        if (selectedPhoto[0] != null) {
            previewImageView.setImage(new Image(new ByteArrayInputStream(selectedPhoto[0])));
        }

        Button uploadPhotoButton = new Button(loc.getString("author.upload_photo_button"));
        uploadPhotoButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(loc.getString("author.upload_photo_title"));
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            java.io.File selectedFile = fileChooser.showOpenDialog(uploadPhotoButton.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }

            try {
                selectedPhoto[0] = Files.readAllBytes(Path.of(selectedFile.toURI()));
                previewImageView.setImage(new Image(new ByteArrayInputStream(selectedPhoto[0])));
            } catch (IOException ex) {
                DialogUtils.showError(loc.getString("dialog.error"), loc.getString("author.photo_load_error"));
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label(loc.getString("author.first_name")), 0, 0);
        form.add(firstNameField, 1, 0);
        form.add(new Label(loc.getString("author.last_name")), 0, 1);
        form.add(lastNameField, 1, 1);
        form.add(new Label(loc.getString("author.year_of_birth")), 0, 2);
        form.add(yearField, 1, 2);
        form.add(new Label(loc.getString("author.nationality")), 0, 3);
        form.add(nationalityField, 1, 3);
        form.add(uploadPhotoButton, 1, 4);
        form.add(previewImageView, 1, 5);

        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return Optional.empty();
        }

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String yearText = yearField.getText().trim();
        String nationality = nationalityField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || yearText.isEmpty() || nationality.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
            return Optional.empty();
        }

        Year yearOfBirth;
        try {
            yearOfBirth = ValidationUtils.validateYear(yearText).orElseThrow();
        } catch (IllegalArgumentException ex) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("author.year_numeric"));
            return Optional.empty();
        }

        return Optional.of(new Author(firstName, lastName, yearOfBirth, nationality, selectedPhoto[0]));
    }

    @FXML
    void deleteButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Author selected = authorsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(loc.getString("confirm.delete_author"));
        confirm.setHeaderText(null);
        confirm.setContentText(loc.getString("confirm.delete_author") + ": " + selected.getFirstName() + " " + selected.getLastName() + "?");
        DialogUtils.applyDialogTheme(confirm);

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                AuthorDao.deleteAuthor(selected);
                LocalizationManager loc2 = LocalizationManager.getInstance();
                DataPreloadService.getInstance().runDbTask(
                        AuthorDao::getAllAuthors,
                        newAuthors -> {
                            authors.setAll(newAuthors);
                            DataPreloadService.getInstance().replaceCachedAuthors(newAuthors);
                            DialogUtils.showInfo(loc2.getString("dialog.info"), loc2.getString("success.author_deleted"));
                        },
                        e -> DialogUtils.showError(loc2.getString("dialog.error"), loc2.getString("error.fetch_authors"))
                );
            }
        });
    }

    private void resetSelectedAuthorPanel() {
        LocalizationManager loc = LocalizationManager.getInstance();
        authorPhotoImageView.setImage(null);
        selectedAuthorNameLabel.setText("-");
        selectedAuthorDetailsLabel.setText("-");
        authorPhotoPlaceholderLabel.setText(loc.getString("author.photo_placeholder"));
        authorPhotoPlaceholderLabel.setVisible(true);
        authorPhotoPlaceholderLabel.setManaged(true);
    }

}
