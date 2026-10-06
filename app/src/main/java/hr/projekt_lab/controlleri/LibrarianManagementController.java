package hr.projekt_lab.controlleri;

import hr.projekt_lab.databaseUtil.LibrarianDao;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LibrarianManagementController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField fullNameField;

    @FXML
    void addLibrarianButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();

        if (username.isBlank() || password.isBlank() || fullName.isBlank()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("librarian.validation"));
            return;
        }

        if (LibrarianDao.findByUsername(username).isPresent()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("librarian.username_exists"));
            return;
        }

        boolean saved = LibrarianDao.insertLibrarian(username, password, fullName);
        if (!saved) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("librarian.add_error"));
            return;
        }

        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();
        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("librarian.add_success"));
    }
}