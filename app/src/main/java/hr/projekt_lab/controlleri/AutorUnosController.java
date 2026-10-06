package hr.projekt_lab.controlleri;

import hr.projekt_lab.databaseUtil.AuthorDao;
import hr.projekt_lab.entities.Author;
import hr.projekt_lab.libraryutils.ValidationUtils;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;

public class AutorUnosController {

    @FXML
    private Text titleText;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField yearOfBirthField;

    @FXML
    private TextField nationalityField;

    @FXML
    private Button saveButton;

    @FXML
    private Button uploadPhotoButton;

    @FXML
    private ImageView authorPhotoImageView;

    @FXML
    private Label authorPhotoPlaceholderLabel;

    @FXML
    private Text photoPanelTitleText;

    private byte[] selectedPhoto;

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        
        titleText.setText(loc.getString("author.unos_title"));
        firstNameField.setPromptText(loc.getString("author.unos_prompt_first_name"));
        lastNameField.setPromptText(loc.getString("author.unos_prompt_last_name"));
        yearOfBirthField.setPromptText(loc.getString("author.unos_prompt_year_of_birth"));
        nationalityField.setPromptText(loc.getString("author.unos_prompt_nationality"));
        saveButton.setText(loc.getString("author.save_button"));
        uploadPhotoButton.setText(loc.getString("author.upload_photo_button"));
        photoPanelTitleText.setText(loc.getString("author.photo_panel_title"));
        authorPhotoPlaceholderLabel.setText(loc.getString("author.photo_preview_placeholder"));
    }

    @FXML
    void uploadPhotoButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(loc.getString("author.upload_photo_title"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(uploadPhotoButton.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        Path filePath = selectedFile.toPath();
        try {
            selectedPhoto = Files.readAllBytes(filePath);
            authorPhotoImageView.setImage(new Image(new ByteArrayInputStream(selectedPhoto)));
            authorPhotoPlaceholderLabel.setVisible(false);
            authorPhotoPlaceholderLabel.setManaged(false);
        } catch (IOException e) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("author.photo_load_error"));
        }
    }

    @FXML
    void saveAuthorButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String firstName=firstNameField.getText().trim();
        String lastName=lastNameField.getText().trim();
        String yearText=yearOfBirthField.getText().trim();
        String nationality=nationalityField.getText().trim();

        if(firstName.isEmpty() || lastName.isEmpty() || yearText.isEmpty() || nationality.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
            return;
        }
        Year yearOfBirth;
        try {
            yearOfBirth = ValidationUtils.validateYear(yearText).orElseThrow();
        } catch (IllegalArgumentException e) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("author.year_numeric"));
            return;
        }

        /*List<Author> authors = new ArrayList<>(
                AuthorJsonUtil.ucitajAutore().orElse(List.of())
        );*/

        Author newAuthor = new Author(firstName, lastName, yearOfBirth, nationality, selectedPhoto);

        /*authors.add(newAuthor);
        AuthorJsonUtil.spremiAutore(authors);*/

        AuthorDao.saveAuthor(newAuthor);
        DataPreloadService.getInstance().refreshAuthorsCache();

        DialogUtils.showInfo(
                loc.getString("dialog.info"), loc.getString("success.author_updated")
        );

        clearFields();
    }
    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        yearOfBirthField.clear();
        nationalityField.clear();
        selectedPhoto = null;
        authorPhotoImageView.setImage(null);
        authorPhotoPlaceholderLabel.setVisible(true);
        authorPhotoPlaceholderLabel.setManaged(true);
    }

}
